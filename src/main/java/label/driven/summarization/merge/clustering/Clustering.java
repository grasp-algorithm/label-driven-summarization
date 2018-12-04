package label.driven.summarization.merge.clustering;

import groovy.lang.Tuple2;
import label.driven.summarization.exception.SummaryGraphConstructionException;
import label.driven.summarization.graph.GraphSession;
import label.driven.summarization.merge.Constants;
import label.driven.summarization.merge.PrefixPropertyConstant;
import label.driven.summarization.merge.classes.CrossEdge;
import label.driven.summarization.schema.Schema;
import label.driven.summarization.schema.json.VertexMergeStrategy;
import oracle.pgql.lang.PgqlException;
import oracle.pgx.api.*;
import oracle.pgx.api.filter.EdgeFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

/**
 * TODO Doc
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/11/18.
 */
public class Clustering {
    private static final Logger LOG = LogManager.getLogger("GLOBAL");
    private static volatile AtomicLong idxEdge = new AtomicLong(-1L);

    private Schema schema;
    private Set<Tuple2<Integer, Integer>> pairs;
    private Map<String, Set<String>> freshLabels;
    private AtomicInteger counterLabels = new AtomicInteger();


    private static void setIdxEdge(Long idxEdgeValue) {
        idxEdge.set(idxEdgeValue);
    }

    public Clustering(Schema schema, int counterLabels) {
        this.schema = schema;
        this.pairs = new HashSet<>();
        this.freshLabels = new HashMap<>();
        this.counterLabels.set(counterLabels + 1);
    }

    /**
     * This procedure groups the existent edges by source, destination and ratio, and merge them saving
     * the labels on the different edges as new properties
     *
     * @param graph the input graph
     *
     * @return a summary with the edges with de criteria described above merged
     * @throws SummaryGraphConstructionException if it's not possible build the summary.
     */
    public PgxGraph clusterEdges(PgxGraph graph) throws SummaryGraphConstructionException {
        setIdxEdge(graph.getNumEdges());

        if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.CLUSTERING_EDGES)) {

            try {

                PgqlResultSet results = graph.queryPgql("SELECT id(x), id(y) MATCH (x) -> (y)");
                for (PgxResult result : results)
                    pairs.add(new Tuple2<>(result.getInteger(1), result.getInteger(2)));

            } catch (ExecutionException | InterruptedException | PgqlException e) {
                LOG.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }

            Set<CrossEdge> edges = new HashSet<>();
            pairs.forEach(tuple -> {
                try {
                    PgxGraph subGraph = graph.filter(new EdgeFilter(String.format("src = %s && dst = %s",
                            tuple.getFirst(), tuple.getSecond())));

                    subGraph.getEdges().forEach(edge -> {
                        try {
                            CrossEdge crossEdge = new CrossEdge((Integer) edge.getSource().getId(),
                                    (Integer) edge.getDestination().getId(), edge.getLabel(),
                                    edge.getProperty(PrefixPropertyConstant.EDGE_RATIO.toString()));
                            crossEdge.setIdxEdge(edge.getId());
                            crossEdge.setWeight(edge.getProperty(PrefixPropertyConstant.EDGE_WEIGHT.toString()));

                            edges.add(crossEdge);
                        } catch (ExecutionException | InterruptedException e) {
                            LOG.error(e.getMessage(), e);
                            Thread.currentThread().interrupt();
                        }
                    });

                } catch (ExecutionException | InterruptedException e) {
                    LOG.error(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            });

            try {
                /* We create a change-set to remove the edges with the same weight, source and destination,
                 * so, we'll group the edges by those three criteria and after that we add one edge and remove all the
                 * correspondence.*/
                return updateSummary(graph, edges);
            } catch (ExecutionException | InterruptedException e) {
                LOG.error(e.getMessage(), e);
                throw new SummaryGraphConstructionException("The summary couldn't be built.");
            }
        }

        LOG.warn("Clustering avoided");
        return graph;
    }


    /**
     * We create a change-set to remove the edges with the same weight, source and destination,
     * so, we'll group the edges by those three criteria and after that we add one edge and remove all the
     * correspondence.
     *
     * @param graph the input graph
     * @param edges the set of edges that will be merged
     *
     * @return a new graph with less edges
     * @throws ExecutionException   if it's not possible build the summary
     * @throws InterruptedException if it's not possible build the summary
     */
    private PgxGraph updateSummary(PgxGraph graph, Set<CrossEdge> edges) throws ExecutionException, InterruptedException {

        GraphChangeSet<Integer> summary = graph.createChangeSet();
        edges.stream().collect(groupingBy(CrossEdge::getIdSrcHN, groupingBy(CrossEdge::getIdDstHN,
                groupingBy(CrossEdge::getRatio)))).forEach((idSrc, list) ->
            list.forEach((idDst, ratios) -> ratios.forEach((ratio, listEdges) -> {

                /* if we have only one edge */
                if (listEdges.size() == 1) {
                    summary.updateEdge(listEdges.get(0).getIdxEdge())
                            .setProperty(PrefixPropertyConstant.EDGE_WEIGHT.toString().concat(Constants
                                    .SEPARATOR_PROPERTY.concat(listEdges.get(0).getLabel())), listEdges.get(0).getWeight())
                            .setProperty(PrefixPropertyConstant.EDGE_WEIGHT.toString(), 0F)
                            .setLabel(listEdges.get(0).getLabel());

                    return;
                }

                EdgeBuilder<Integer> edgeBuilder = summary.addEdge(idxEdge.incrementAndGet(),
                        idSrc, idDst);

                Map<String, Float> sumWeight = new HashMap<>();
                Set<String> multiLabel = new HashSet<>();

                listEdges.forEach(labels -> {
                    multiLabel.add(labels.getLabel());
                    summary.removeEdge(labels.getIdxEdge());
                    sumWeight.put(labels.getLabel(), sumWeight.getOrDefault(labels.getLabel(), 0F) +
                            labels.getWeight());
                });

                /* Here we retrieve the label of the edge, there is two possible cases, the first one where we only
                 * have 1 label on the edge, which it means basically that the hyperedge is going to keep this label,
                 * and the other case is when the edge is multi-label which means that we need to generate a new
                 * fresh label which is going two represent the list of this labels */
                if (multiLabel.size() > 1) {
                    AtomicReference<String> newFreshLabel = new AtomicReference<>(null);
                    freshLabels.entrySet().stream().filter(entry -> entry.getValue().equals(multiLabel)).findAny()
                            .ifPresent(entry -> newFreshLabel.set(entry.getKey()));

                    /* We need to generate a new fresh label */
                    if (newFreshLabel.get() == null) {
                        newFreshLabel.set(GraphSession.LABEL_PREFIX.concat(String.valueOf(counterLabels.incrementAndGet())));
                        freshLabels.put(newFreshLabel.get(), multiLabel.stream().sorted().collect(Collectors.toSet()));
                    }

                    edgeBuilder.setLabel(newFreshLabel.get());

                } else {
                    multiLabel.forEach(edgeBuilder::setLabel);
                }

                edgeBuilder.setProperty(PrefixPropertyConstant.EDGE_RATIO.toString(), ratio);

                /* Here we save the weights of different labels in different properties of the edge */
                sumWeight.forEach((k, v) -> edgeBuilder.setProperty(
                        PrefixPropertyConstant.EDGE_WEIGHT.toString().concat(Constants
                                .SEPARATOR_PROPERTY.concat(k)), v));

            }))
        );

        return summary.build();
    }

    public Map<String, Set<String>> getFreshLabels() {
        return freshLabels;
    }
}
