package label.driven.summarization.merge.frequency;

import label.driven.summarization.exception.IdDestinationNotFound;
import label.driven.summarization.exception.MergeStrategyException;
import label.driven.summarization.exception.PropertyNotFoundException;
import label.driven.summarization.exception.SummaryGraphConstructionException;
import label.driven.summarization.graph.GraphUtils;
import label.driven.summarization.graph.Session;
import label.driven.summarization.merge.Constants;
import label.driven.summarization.merge.classes.CrossEdge;
import label.driven.summarization.merge.classes.FrequencyEdge;
import label.driven.summarization.merge.common.HyperNode;
import label.driven.summarization.merge.common.HyperNodeImpl;
import label.driven.summarization.merge.PrefixPropertyConstant;
import label.driven.summarization.merge.frequency.heuristics.InnerMergeEquality;
import label.driven.summarization.merge.frequency.heuristics.InnerMergeFreqLabel;
import label.driven.summarization.merge.frequency.heuristics.WeightMergeHeuristic;
import label.driven.summarization.schema.Schema;
import label.driven.summarization.schema.json.VertexMergeStrategy;
import oracle.pgql.lang.PgqlException;
import oracle.pgx.api.GraphBuilder;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.PgxVertex;
import oracle.pgx.api.VertexBuilder;
import oracle.pgx.api.filter.EdgeFilter;
import oracle.pgx.api.filter.VertexFilter;
import oracle.pgx.config.IdGenerationStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

/**
 * This class implements the merge strategy using the frequency of the weights.
 * Two approach are proposed:
 * - Merging the sinks: where we merge the nodes with out_degree = 0 and the same value on the node_weight property.
 * - Merging the source: where we merge the nodes with the same set of labels on the outgoing edges and the same
 * node_weight property. This second strategy can be relaxed using the epsilon parameter.
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/22/18.
 */
public class Frequency {
    private static final Logger LOG = LogManager.getLogger("GLOBAL");
    private static volatile AtomicInteger idxVertex = new AtomicInteger(-1);
    private static volatile AtomicInteger idxEdge = new AtomicInteger(-1);

    private Map<String, Set<Integer>> groupingByHyperNodeWithOutDegree;
    private Map<String, Set<Integer>> groupingByHyperNodeWithNoneOutDegree;

    private GraphBuilder<Integer> summaryBuilder;
    private Map<Integer, HyperNode> hyperNodes;
    private Set<String> vertexProperties;
    private Schema schema;
    private PgxGraph graph;

    /**
     * @param session the current session
     * @param graph   the input graph which can be a summary already.
     * @param schema  the schema of the input graph.
     */
    public Frequency(Session session, PgxGraph graph, Schema schema) {
        this.summaryBuilder = session.getSession().createGraphBuilder(IdGenerationStrategy.USER_IDS, IdGenerationStrategy.USER_IDS);
        this.schema = schema;
        this.hyperNodes = new HashMap<>();
        this.groupingByHyperNodeWithNoneOutDegree = new HashMap<>();
        this.groupingByHyperNodeWithOutDegree = new HashMap<>();
        this.graph = graph;
        this.vertexProperties = initializeProperties();
    }

    /**
     * Compute the set of properties on the input graph.
     *
     * @return the set of properties
     */
    private Set<String> initializeProperties() {
        Set<String> properties = new HashSet<>();
        graph.getVertexProperties().forEach(vp -> properties.add(vp.getName()));

        return properties;
    }

    /**
     * This methods apply the heuristics to merge the nodes defined in the configuration file.
     *
     * @return the same instance.
     * @throws MergeStrategyException if the merge stratedy fails.
     */
    public Frequency mergeByFrequency() throws MergeStrategyException {

        try {
            frequencyMergingOfSinkVertices();
            frequencyMergingOfSourceVertices();


            /* We set the property `grouping` of all the vertices to `-1` */
            graph = GraphUtils.cleanGroupingProperty(graph, true);

            /* Update of the grouping on the original graph with the new `grouping` (idHN)*/
            Map<String, Set<Integer>> toUpdate = new HashMap<>();
            hyperNodes.forEach((part, hyperNode) -> {
                Set<Integer> listOfIds = hyperNode.getVertices().stream().map(v -> (Integer) v.getId())
                        .collect(Collectors.toSet());
                toUpdate.put(String.valueOf(part), listOfIds);
            });

            /* After the merge of all the possible vertices respecting the given conditions we update
             * the graph with the new hyper-node identifiers */
            graph = GraphUtils.updateGraph(graph, toUpdate);
            computeLabeledOccurrencesOnEachHN();

            /* Then we add the missing nodes (the nodes that couldn't be merged) and we update the graph */
            toUpdate.clear();
            toUpdate.putAll(addMissingNodes());
            graph = GraphUtils.updateGraph(graph, toUpdate);

            return this;

        } catch (ExecutionException | InterruptedException | PgqlException e) {
            LOG.error(e.getMessage(), e);
            throw new MergeStrategyException("The merge among the vertices couldn't be executed.");
        }
    }

    /**
     * This method congregates all the nodes with the same frequency and out_degree = 0
     *
     * @throws ExecutionException   if the pgql query fails
     * @throws InterruptedException if the pgql query fails
     * @throws PgqlException        if the pgql query fails
     */
    private void frequencyMergingOfSinkVertices() throws ExecutionException, InterruptedException, PgqlException {

        if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.FREQUENCY_SINK_NODES)) {

            AtomicLong numberOfSNMerged = new AtomicLong(0L);

            if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.WEIGHT_MERGE))
                new WeightMergeHeuristic().mergeSinkVertices(schema, graph)
                        .forEach(set -> {
                            computeHyperNodes(set, schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.TARGET_MERGE));
                            numberOfSNMerged.addAndGet(set.size());
                        });

            if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.INNER_MERGE_EQUALITY))
                new InnerMergeEquality().mergeSinkVertices(schema, graph)
                        .forEach(set -> {
                            computeHyperNodes(set, schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.TARGET_MERGE));
                            numberOfSNMerged.addAndGet(set.size());
                        });

            if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.INNER_MERGE_FREQ_LABEL))
                new InnerMergeFreqLabel().mergeSinkVertices(schema, graph)
                        .forEach(set -> {
                            computeHyperNodes(set, schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.TARGET_MERGE));
                            numberOfSNMerged.addAndGet(set.size());
                        });
        }
    }


    /**
     * We congregate all the nodes with the same weight (+- epsilon) and the same set of labels on
     * the outgoing edges.
     */
    private void frequencyMergingOfSourceVertices() throws ExecutionException, InterruptedException, PgqlException {

        if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.FREQUENCY_SOURCE_NODES)) {

            List<Set<PgxVertex>> labelsByVertex = null;

            /* Because we can merge two different frequencies (epsilon parameter different of 0) we have to
             * compute the avg of those, which will generate new frequencies */

            if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.WEIGHT_MERGE))
                labelsByVertex = new WeightMergeHeuristic().mergeSourceAndIntermediateVertices(schema, graph);

            else if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.INNER_MERGE_EQUALITY))
                labelsByVertex = new InnerMergeEquality().mergeSourceAndIntermediateVertices(schema, graph);

            else if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.INNER_MERGE_FREQ_LABEL))
                labelsByVertex = new InnerMergeFreqLabel().mergeSourceAndIntermediateVertices(schema, graph);


            AtomicLong numberOfSNMerged = new AtomicLong(0L);

            /* Once we compute the list of nodes by label set, we congregate the vertices. */
            Objects.requireNonNull(labelsByVertex).forEach(v -> {
                computeHyperNodes(v, true);
                numberOfSNMerged.addAndGet(v.size());
            });

        }
    }


    /**
     * This procedure add all the vertices that doesn't respect the above conditions, so they cannot be merge, but
     * they are included in the summary.
     */
    private Map<String, Set<Integer>> addMissingNodes() {
        Map<String, Set<Integer>> toUpdate = new HashMap<>();

        try {

            /* We look for all the vertices with grouping = 0 which means that they don't have assigned any hyper-node  */
            graph.getVertices(new VertexFilter(String.format("vertex.%s = %s", PrefixPropertyConstant.GROUPING.toString(),
                    Constants.GROUPING_DEFAULT_VALUE)))
                    .forEach(v -> {

                        /* For each node in the input graph we create a new hyper-node with only one node */
                        VertexBuilder vertexBuilder = summaryBuilder.addVertex(idxVertex.incrementAndGet());
                        vertexBuilder.setProperty(PrefixPropertyConstant.NODE_WEIGHT_FREQUENCY.toString(), 1);
                        AtomicInteger grouping = new AtomicInteger();

                        AtomicReference<Float> nodeWeightEval = new AtomicReference<>(0F);
                        /* We preserve all the properties of the super-node */
                        vertexProperties.forEach(vp -> {
                            try {
                                if (vp.equals(PrefixPropertyConstant.NODE_WEIGHT_EVALUATION.toString()))
                                    nodeWeightEval.set(v.getProperty(vp));

                                grouping.set((Integer) v.getId());
                                vertexBuilder.setProperty(vp, v.getProperty(vp));
                            } catch (ExecutionException | InterruptedException e) {
                                LOG.error(e.getMessage(), e);
                                Thread.currentThread().interrupt();
                            }
                        });

                        /* The average is equals to the weight on the SN, because the NODE_WEIGHT_FREQUENCY is 1*/
                        vertexBuilder.setProperty(PrefixPropertyConstant.NODE_WEIGHT_EVALUATION_AVG.toString(),
                                nodeWeightEval.get());
                        vertexBuilder.setProperty(PrefixPropertyConstant.GROUPING.toString(), (float) grouping.get());
                        hyperNodes.put(idxVertex.get(), new HyperNodeImpl(grouping.get(), idxVertex.get(),
                                Collections.singleton(v)));

                        /* We save the node in the structure `toUpdate` to modify the grouping of the super-node in the
                         * input graph */
                        toUpdate.put(String.valueOf(idxVertex.get()), Collections.singleton((Integer) v.getId()));

                        if (graph.getVertex(v.getId()).getOutDegree() > 0L)
                            groupingByHyperNodeWithOutDegree.put(String.valueOf(idxVertex.get()),
                                    Collections.singleton((Integer) v.getId()));
                        else
                            groupingByHyperNodeWithNoneOutDegree.put(String.valueOf(idxVertex.get()),
                                    Collections.singleton((Integer) v.getId()));
                    });
        } catch (ExecutionException | InterruptedException e) {
            LOG.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

        return toUpdate;
    }

    /**
     * This method congregate the list of vertices send as parameter in one hyper-node
     *
     * @param pgxVertices   the list of vertices that will need be merged.
     * @param withOutDegree this parameter indicate for which heuristic it's been called (merge the sources or merge
     *                      the sinks)
     */
    private void computeHyperNodes(Set<PgxVertex> pgxVertices, boolean withOutDegree) {
        /* We save all the groupings included on each hyper-node!, this will help us for the computation
         * of the cross-edges @see Frequency#computeEdges(Map)  */

        Set<Integer> listIds = new HashSet<>();
        AtomicReference<Float> weightAvg = new AtomicReference<>(0F);

        for (PgxVertex v : pgxVertices) {
            listIds.add((Integer) v.getId());
            try {
                weightAvg.accumulateAndGet(
                        (Float) v.getProperty(PrefixPropertyConstant.NODE_WEIGHT_EVALUATION.toString()),
                        (a, b) -> a + b);
            } catch (ExecutionException | InterruptedException e) {
                LOG.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }

        weightAvg.set(weightAvg.get() / pgxVertices.size());

        hyperNodes.put(idxVertex.incrementAndGet(), new HyperNodeImpl(getGrouping(pgxVertices), idxVertex.get(),
                new HashSet<>(pgxVertices), weightAvg.get()));

        if (withOutDegree)
            groupingByHyperNodeWithOutDegree.put(String.valueOf(idxVertex.get()), listIds);
        else
            groupingByHyperNodeWithNoneOutDegree.put(String.valueOf(idxVertex.get()), listIds);
    }


    /**
     * @param pgxVertices the list of vertices which will be merged.
     *
     * @return the grouping of one vertex.
     */
    private Integer getGrouping(Set<PgxVertex> pgxVertices) {
        for (PgxVertex v : pgxVertices) {
            try {
                return ((Float) v.getProperty(PrefixPropertyConstant.GROUPING.toString())).intValue();
            } catch (ExecutionException | InterruptedException e) {
                LOG.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }

        throw new PropertyNotFoundException(String.format("The property %s wasn't found.",
                PrefixPropertyConstant.GROUPING.toString()));
    }

    /**
     * This methods compute the edges on this new summary,
     *
     * @return the graph with the edges calibrated
     * @throws SummaryGraphConstructionException if it's not possible build the summary
     */
    public PgxGraph calibrateEdges() throws SummaryGraphConstructionException {

        Map<Integer, Set<FrequencyEdge>> edgesToBeMerged = new HashMap<>();

        groupingByHyperNodeWithOutDegree.forEach((idVtxHN, listSN) -> {
            try {
                /*
                 * For all the outgoing edges from a hyper-node to different super-nodes we save
                 * in `edgesToBeMerged` the set of edges with the frequency on the destination vertex and
                 * the weight and label of the edge
                 */
                graph.filter(new EdgeFilter(String.format("src.%s == %s", PrefixPropertyConstant.GROUPING.toString(),
                        idVtxHN))).getEdges().forEach(edge -> {

                    Integer idVertexHN = Integer.parseInt(idVtxHN);

                    try {
                        Set<FrequencyEdge> edges = edgesToBeMerged.getOrDefault(idVertexHN, new HashSet<>());

                        edges.add(new FrequencyEdge(idVertexHN, (Integer) edge.getSource().getId(),
                                findIdHNDestination((Integer) edge.getDestination().getId()),
                                (Integer) edge.getDestination().getId(), edge.getLabel(),
                                edge.getProperty(PrefixPropertyConstant.EDGE_RATIO.toString()),
                                edge.getProperty(PrefixPropertyConstant.EDGE_WEIGHT.toString()),
                                edge.getSource().getProperty(PrefixPropertyConstant.NODE_WEIGHT_EVALUATION.toString()),
                                edge.getDestination().getProperty(PrefixPropertyConstant.NODE_WEIGHT_EVALUATION.toString())));

                        edgesToBeMerged.put(idVertexHN, edges);

                    } catch (ExecutionException | InterruptedException e) {
                        LOG.error(e.getMessage(), e);
                        Thread.currentThread().interrupt();
                    }

                });
            } catch (InterruptedException | ExecutionException e) {
                LOG.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        });

        /*
         * For each edge in `edgesToBeMerged` we made a group by the id source and id of the destination of the
         * hyper-node and the label of the edge.
         */
        Map<CrossEdge, Float> avgCrossEdges = new HashMap<>();
        Map<CrossEdge, Float> sumOfAllEdgesWeights = new HashMap<>();

        edgesToBeMerged.forEach((idSrcHN, crossEdges) -> crossEdges.stream().collect(groupingBy(FrequencyEdge::getIdDstHN,
                groupingBy(FrequencyEdge::getLabel)))
                .forEach((idDstHN, edgesByLabel) ->

                    /*
                     * And them, for each edge, we save the sum of the multiplication between the frequency and the
                     * weight of the edge, and the sum of all the frequencies. Thus, we can compute the average of the
                     * new weight of the edge by `label` and `idDstHN`, it's important point out than the variable
                     * `idDstHN` is representing the id of the HN.
                     *
                     * It's important to note that only one cross-edge by label between two hyper-nodes is allowed.
                     * We cannot compute multi-labels.
                     */
                    edgesByLabel.forEach((label, edges) -> {

                        CrossEdge currentCrossEdgeComputation = new CrossEdge(idSrcHN, idDstHN, label);

                        /* We compute the sum of all the SN destination weight that it'll be the divisor */
                        AtomicInteger sumOfDstWeights = new AtomicInteger(0);

                        /* We compute the sum of all weight edges */
                        AtomicReference<Float> sumOfEdgeWeights = new AtomicReference<>(0F);

                        edges.forEach(edge -> {
                            sumOfDstWeights.addAndGet(edge.getNodeWeightDst());

                            /* We save the sum of edge-weights of each crossEdge, because later we're going to add
                            the property to the HN*/
                            sumOfEdgeWeights.accumulateAndGet(edge.getEdgeWeight(), (a, b) -> a + b);

                            /*
                             * Here we save the sum of relative weight between two different grouping `idSrcHN` & `idDstHN`
                             */
                            avgCrossEdges.put(currentCrossEdgeComputation, avgCrossEdges.getOrDefault(currentCrossEdgeComputation,
                                    0F) + edge.getNodeWeightDst()
                                    * edge.getEdgeRatio());
                        });

                        float totalValue = avgCrossEdges.getOrDefault(currentCrossEdgeComputation,
                                0F);
                        avgCrossEdges.put(currentCrossEdgeComputation, totalValue / sumOfDstWeights.get());
                        sumOfAllEdgesWeights.put(currentCrossEdgeComputation, sumOfEdgeWeights.get());

                    })
                ));

        /*
         * Here we compute the final cross-edges with the weighted average.
         */
        avgCrossEdges.forEach((crossEdge, edgeRatio) -> {
            if (edgeRatio > schema.getConfig().getTheta())
                summaryBuilder.addEdge(idxEdge.incrementAndGet(), crossEdge.getIdSrcHN(), crossEdge.getIdDstHN())
                        .setProperty(PrefixPropertyConstant.EDGE_RATIO.toString(), edgeRatio)
                        .setProperty(PrefixPropertyConstant.EDGE_WEIGHT.toString(), sumOfAllEdgesWeights.get(crossEdge))
                        .setLabel(crossEdge.getLabel());
        });


        PgxGraph summary;
        try {
            summary = summaryBuilder.build();
        } catch (ExecutionException | InterruptedException e) {
            LOG.error(e.getMessage(), e);
            throw new SummaryGraphConstructionException("Summary graph couldn't be built.");
        }
        return summary;
    }

    /**
     * This method find the id of the hyper-node, given the id of the super-node
     *
     * @param idDst represents the SN identifier
     *
     * @return the id of the hyper-node that includes the current destination
     */
    private int findIdHNDestination(Integer idDst) {
        AtomicInteger idDestination = new AtomicInteger(-1);
        // find the id in the current summary not on the graph!
        groupingByHyperNodeWithOutDegree.entrySet().stream().filter(pa -> pa.getValue().contains(idDst))
                .findAny().ifPresent(pa -> idDestination.set(Integer.parseInt(pa.getKey())));

        if (idDestination.get() == -1)
            groupingByHyperNodeWithNoneOutDegree.entrySet().stream().filter(pa -> pa.getValue().contains(idDst))
                    .findAny().ifPresent(pa -> idDestination.set(Integer.parseInt(pa.getKey())));

        if (idDestination.get() == -1)
            throw new IdDestinationNotFound("Id destination NOT FOUND.");

        return idDestination.get();
    }


    /**
     * TODO reduce the number of hyperNodes, we don't need this for the isolated nodes
     *
     */
    private void computeLabeledOccurrencesOnEachHN() {

        hyperNodes.forEach((idHN, hyperNode) -> {

            MergePropertyStrategy mergeStrategy = new MergePropertyStrategy(graph);

            /* setting of all the new properties to the super-node. */
            VertexBuilder vertexBuilder = summaryBuilder.addVertex(idHN);
            vertexBuilder.setProperty(PrefixPropertyConstant.NODE_WEIGHT_FREQUENCY.toString(),
                    hyperNode.getVertices().size());
            vertexBuilder.setProperty(PrefixPropertyConstant.NODE_WEIGHT_EVALUATION_AVG.toString(),
                    hyperNode.getAvgWeight());

            Map<String, Float> newProperties = mergeStrategy.mergeProperty(schema, vertexProperties,
                    hyperNode.getVertices());
            newProperties.forEach(vertexBuilder::setProperty);

            Map<String, Float> frontierTraversalsProps = mergeStrategy.computeTraversalFrontiers(schema,
                    hyperNode.getVertices());
            frontierTraversalsProps.forEach(vertexBuilder::setProperty);

        });
    }

    public Map<Integer, HyperNode> getHyperNodes() {
        return hyperNodes;
    }
}
