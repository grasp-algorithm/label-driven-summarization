package label.driven.summarization.merge.evaluation;

import label.driven.summarization.exception.MergeStrategyException;
import label.driven.summarization.exception.SummaryGraphConstructionException;
import label.driven.summarization.graph.Session;
import label.driven.summarization.merge.PrefixPropertyConstant;
import label.driven.summarization.merge.common.SuperNode;
import label.driven.summarization.merge.common.SuperNodeImpl;
import label.driven.summarization.merge.evaluation.histogram.Histogram;
import label.driven.summarization.merge.evaluation.histogram.HistogramBuilder;
import label.driven.summarization.ratio.EdgeRatioComputation;
import label.driven.summarization.schema.Schema;
import label.driven.summarization.schema.json.VertexMergeStrategy;
import label.driven.summarization.util.EdgeRatioRow;
import oracle.pgx.api.*;
import oracle.pgx.api.filter.EdgeFilter;
import oracle.pgx.api.filter.VertexFilter;
import oracle.pgx.config.IdGenerationStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is responsible to evaluate the Kleene star inside of each grouping.
 * Additionally, we have to built an histogram for each attribute value.
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/16/18.
 */
public class Evaluation {
    private static final Logger LOG = LogManager.getLogger("GLOBAL");
    private static final String NO_LABEL_ASSOCIATED = "NO_LABEL_ASSOCIATED";

    private static volatile AtomicInteger idxVertex = new AtomicInteger(-1);
    private static volatile AtomicInteger idxEdge = new AtomicInteger(-1);

    private Session session;
    private Schema schema;
    private GraphBuilder<Integer> summaryBuilder;
    private PgxGraph summary;
    private Map<Integer, SuperNode> superNodes;


    /**
     * @param graphSession Graph session.
     */
    public Evaluation(Schema schema, Session graphSession) {
        session = graphSession;
        this.schema = schema;
        summaryBuilder = session.getSession().createGraphBuilder(IdGenerationStrategy.USER_IDS, IdGenerationStrategy.USER_IDS);
        superNodes = new HashMap<>();
    }

    /**
     * We evaluate using Kleene star by the label of each grouping.
     *
     * @param grouping     the list of grouping
     * @param correspondence the correspondence between some grouping and the labels
     */
    public void evaluate(Set<Integer> grouping, Map<Integer, String> correspondence)
            throws SummaryGraphConstructionException, MergeStrategyException {

        if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.EVALUATION)) {

            try {
                for (Integer currentGrouping : grouping) {

                    /* It retrieves the label which corresponds to the grouping, it's possible that none label was
                     * founded (e.g. isolated nodes) so, we divide the implementation */
                    String label = correspondence.getOrDefault(currentGrouping, NO_LABEL_ASSOCIATED);

                    if (!label.equals(NO_LABEL_ASSOCIATED))
                        computeTheConnectedComponents(currentGrouping, label);

                    else
                        /* if not label is associated we create a super node with only one node each. */
                        computeTheConnectedComponents(currentGrouping);
                }

                summary = summaryBuilder.build();

            } catch (ExecutionException | InterruptedException e) {
                LOG.error(e.getMessage(), e);
                throw new SummaryGraphConstructionException("Summary graph couldn't be built.");
            }

        } else {
            /* The evaluation step is required */
            throw new MergeStrategyException(String.format("This vertex merge strategy is not allowed. " +
                            "To be able to use this method specify the strategy %s on the schema file.",
                    VertexMergeStrategy.EVALUATION.toValue()));
        }

        LOG.debug("Computation of super-nodes finished.");
    }


    private void computeTheConnectedComponents(int grouping) throws ExecutionException, InterruptedException {
        computeTheConnectedComponents(grouping, null);
    }

    private void computeTheConnectedComponents(int grouping, String label) throws ExecutionException, InterruptedException {

        try (Analyst analyst = session.getSession().createAnalyst()) {

            PgxGraph subGraphByGroupingAndLabel;
            if (label != null)
                subGraphByGroupingAndLabel = session.getGraph().filter(new VertexFilter(
                        String.format("vertex.%1$s == %2$s", PrefixPropertyConstant.GROUPING.toString(), grouping))
                        .intersect(new EdgeFilter(String.format("edge.label() == '%s'", label))));
            else
                subGraphByGroupingAndLabel = session.getGraph().filter(new VertexFilter(String.format("vertex.%1$s == %2$s",
                        PrefixPropertyConstant.GROUPING.toString(), grouping)));

            /* We compute the connected components inside the grouping, taking into account only the label of the
            grouping */
            Partition<Integer> partition = analyst.wcc(subGraphByGroupingAndLabel);

            for (int i = 0; i < partition.size(); i++)
                addNewSuperNode(grouping, partition.getPartitionByIndex(i));

        }
    }


    /**
     * This method compute a new super-node merging the input collection of vertices.
     *
     * @param grouping      the grouping where is included the super-node.
     * @param innerVertices the list of vertices that will be merged.
     */
    private void addNewSuperNode(int grouping, VertexCollection<Integer> innerVertices) {

        /* built of the histogram */
        Histogram histogram = new HistogramBuilder(schema)
                .setVertices(innerVertices)
                .setSession(session)
                .setGrouping(grouping)
                .build();
        Map<String, Float> vertexProperties = histogram.getVertexEdgeProperties();

        /* setting of all the new properties to the super-node. */
        VertexBuilder vertexBuilder = summaryBuilder.addVertex(idxVertex.incrementAndGet());
        vertexProperties.forEach(vertexBuilder::setProperty);

        /* deep copy, not only the reference */
        superNodes.put(idxVertex.get(), new SuperNodeImpl(grouping, idxVertex.get(),
                (VertexSet<Integer>) innerVertices.clone()));
    }

    /**
     * This method receive a map with the values of the participation ratio and uses it to
     * add these edges to the summary.
     *
     * @param participationRatioMap the map of participation ratio by grouping
     *
     * @see EdgeRatioComputation#computeProbabilityOfArriveTo()
     */
    public PgxGraph computeEdges(Map<EdgeRatioRow, Float> participationRatioMap) throws SummaryGraphConstructionException, MergeStrategyException {

        if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.EVALUATION)) {
            try {
                GraphChangeSet<Integer> changeSet = summary.createChangeSet(IdGenerationStrategy.USER_IDS, IdGenerationStrategy.USER_IDS);

                participationRatioMap.forEach((k, v) ->
                        changeSet.addEdge(idxEdge.incrementAndGet(), k.getGroupI(), k.getGroupJ())
                        .setLabel(k.getLabel())
                        .setProperty(PrefixPropertyConstant.EDGE_RATIO.toString(), v)
                        .setProperty(PrefixPropertyConstant.EDGE_WEIGHT.toString(), k.getEdgeWeight()));

                summary = changeSet.build();
                LOG.debug("Computation of super-edges finished.");

                return summary;
            } catch (ExecutionException | InterruptedException e) {
                throw new SummaryGraphConstructionException("Graph cannot be built.");
            }
        }

        /* The evaluation step is required */
        throw new MergeStrategyException(String.format("This vertex merge strategy is not allowed. "
                        + "To be able to use this method specify the strategy %s on the schema file.",
                VertexMergeStrategy.EVALUATION.toValue()));
    }

    /**
     * Return the super-nodes.
     *
     * @return the super-nodes
     */
    public Map<Integer, SuperNode> getSuperNodes() {
        return superNodes;
    }

    /**
     * Return the graph summary after the evaluation step.
     *
     * @return return the graph summary after the step of evaluation
     */
    public PgxGraph getSummary() {
        return summary;
    }

}