package label.driven.summarization.merge.evaluation.histogram;

import label.driven.summarization.graph.GraphUtils;
import label.driven.summarization.graph.Session;
import label.driven.summarization.merge.Constants;
import label.driven.summarization.merge.PrefixPropertyConstant;
import label.driven.summarization.merge.evaluation.histogram.mutation.Filter;
import label.driven.summarization.merge.evaluation.histogram.mutation.FilterImpl;
import label.driven.summarization.merge.evaluation.histogram.properties.LabelParticipationProperty;
import label.driven.summarization.merge.evaluation.histogram.properties.TraversalFrontierProperty;
import label.driven.summarization.schema.Schema;
import oracle.pgql.lang.PgqlException;
import oracle.pgx.api.PgqlResultSet;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.PgxResult;
import oracle.pgx.api.VertexCollection;
import oracle.pgx.api.filter.EdgeFilter;
import oracle.pgx.api.filter.VertexFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/22/18.
 */
public class HistogramImpl implements Histogram {

    private static final Logger LOG = LogManager.getLogger("GLOBAL");

    private Map<String, Integer> labelsOccurrences;
    private Map<String, Integer> counterByProperty;
    private Map<String, Float> vertexEdgeProperties = new HashMap<>();
    private Session session;
    private int grouping;
    private Schema schema;

    /**
     * @param builder the builder of the histogram
     */
    HistogramImpl(HistogramBuilder builder) {
        this.labelsOccurrences = new HashMap<>();
        this.counterByProperty = new HashMap<>();
        this.schema = builder.getSchema();
        this.session = builder.getSession();
        this.grouping = builder.getGrouping();

        initializeCounterByProperty();
    }

    /**
     * For each possible value of each label, we initialize the structure `counterByProperty`, using the schema
     * information.
     * <p>
     * We save the property under the format (LABEL_POSSIBLE_VALUE, 0).
     */
    private void initializeCounterByProperty() {
        List<String> labelsSchema = schema.getAllLabels();
        for (String label : labelsSchema) {
            schema.getAllPossibleValuesByLabel(label).forEach(pv ->
                    counterByProperty.put(label.concat(Constants.SEPARATOR_PROPERTY).concat(pv), 0));
        }
    }


    @Override
    public void computeGrouping(VertexCollection<Integer> pgxVertices) {
        vertexEdgeProperties.put(PrefixPropertyConstant.GROUPING.toString(), (float) grouping);
    }

    @Override
    public void computeVertexPropertiesOnHistogram(VertexCollection<Integer> pgxVertices) {
        int count = pgxVertices.size();

        /* For each property we put the number of occurrences divided by the label, for example for the property `sex`
         * we only divide the number of occurrences of each possible value of this property by the number of labels
         * which use this property */
        counterByProperty.forEach((k, v) -> {

            /* Compute the percentage of occurrences of this property on this super-node, which is basically, the number
             * of occurrences of the property values divided by the number of vertex than use this property  */
            String label = extractLabel(k);
            String labelWithPrefix = PrefixPropertyConstant.LABEL.toString()
                    .concat(Constants.SEPARATOR_PROPERTY).concat(label);
            vertexEdgeProperties.put(k, computeValue(v, labelsOccurrences.getOrDefault(labelWithPrefix, 0)));

        });

        /* Compute the inner-nodes */
        vertexEdgeProperties.put(PrefixPropertyConstant.NODE_WEIGHT_EVALUATION.toString(), (float) pgxVertices.size());

        /* We put in the properties the percentage of occurrences of each label divided by the total number of
        vertices, we add the keyword LABEL before the name */
        labelsOccurrences.forEach((k, v) -> vertexEdgeProperties.put(k, computeValue(v, count)));
    }


    @Override
    public void computeEdgePropertiesOnHistogram(VertexCollection<Integer> pgxVertices) {

        try (Filter filter = new FilterImpl()) {
            PgxGraph subGraphForRequestFiltered = filter.apply(session, pgxVertices, grouping);

            float innerEdges = (float) subGraphForRequestFiltered.getNumEdges();
            vertexEdgeProperties.put(PrefixPropertyConstant.NUMBER_OF_INNER_EDGES.toString(), innerEdges);

            Set<String> setOfInnerLabels = new HashSet<>();
            if (pgxVertices.size() > 1) {
                PgqlResultSet results = subGraphForRequestFiltered.queryPgql("SELECT label(e), COUNT(*) " +
                        "MATCH () -[e]-> () GROUP BY label(e) ");
                for (PgxResult result : results) {
                    setOfInnerLabels.add(result.getString(1));
                    vertexEdgeProperties.put(PrefixPropertyConstant.PERCENTAGE.toString().concat(Constants.SEPARATOR_PROPERTY)
                            .concat(result.getString(1)), (float) result.getLong(2) / innerEdges);
                }

//                if (!schema.isComputeConcatenationProperties())
                    computeReachabilityProperties(subGraphForRequestFiltered, setOfInnerLabels, pgxVertices.size());
            }

            computeConcatenationProperties(pgxVertices);

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

    }

    /**
     * This method compute the properties used for the concatenation queries
     *
     * @param pgxVertices the set of vertices that will be merged
     * @throws ExecutionException if it's not possible build the filter.
     * @throws InterruptedException if it's not possible build the filter.
     */
    private void computeConcatenationProperties(VertexCollection<Integer> pgxVertices) throws ExecutionException, InterruptedException {
        if (schema.isComputeConcatenationProperties()) {

            PgxGraph subGraphWithOneHop = session.getGraph().filter(new VertexFilter(String.format("vertex.%1$s == %2$s",
                    PrefixPropertyConstant.GROUPING.toString(), grouping)).union(new EdgeFilter(
                    String.format("any.%1$s == %2$s", PrefixPropertyConstant.GROUPING.toString(), grouping))));

            PgxGraph filteredGraph = GraphUtils.getFilteredGraph(schema, subGraphWithOneHop);
            try (Filter filter = new FilterImpl()) {

                PgxGraph subGraphWithOneHopFiltered = filter.apply(session, filteredGraph, pgxVertices, filteredGraph.getVertices(),
                        grouping, true);

                if (subGraphWithOneHopFiltered.getVertexProperty(Constants.BLOCKING_PROPERTY) != null) {
                    if (pgxVertices.size() > 1)
                        vertexEdgeProperties.putAll(LabelParticipationProperty.computeParticipationLevelByLabel(schema,
                                subGraphWithOneHopFiltered));

                    vertexEdgeProperties.putAll(TraversalFrontierProperty.computeFrontierTraversalsByLabel(schema,
                            subGraphWithOneHopFiltered));
                }


            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }

            if (!schema.isAllLabelsAllowed())
                filteredGraph.destroy();

            subGraphWithOneHop.destroy();
        }

    }


    /**
     * This method compute the three properties of reachability:
     * - c(l): the number of paths (computing l+) inside of the SN with the label l.
     * - p_out(l): the number of of paths from the inside of the SN to the others SN.
     * - p_in(l): the sum of the in_degree of the frontier nodes which have at least one edge from outside of the SN.
     *
     * @param graph the graph
     */
    private void computeReachabilityProperties(PgxGraph graph, Set<String> labels, int numberOfInnerNodes) {

        try {

            for (String label : labels)
                computeReachabilityCount(graph, label, numberOfInnerNodes);

        } catch (PgqlException | ExecutionException | InterruptedException e) {
            LOG.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }


    /**
     * Compute how many of paths are inside of the super-node by label.
     *
     * @param graph the subGraph inside of the super-node
     * @param label the current label of the super-node
     * @param numberOfInnerNodes the number of inner nodes.
     *
     * @throws PgqlException if the property cannot be computed.
     * @throws ExecutionException if the property cannot be computed.
     * @throws InterruptedException if the property cannot be computed.
     */
    private void computeReachabilityCount(PgxGraph graph, String label, int numberOfInnerNodes) throws PgqlException, ExecutionException, InterruptedException {

        /* if the number of inner-nodes is equals to 1 we already know that the reachability count will be
                equals to 0*/
        if (numberOfInnerNodes > 1) {

            /* Computing the reachability count, the number of `inner-paths` on the SN by label */
            PgqlResultSet results = graph.queryPgql(" SELECT COUNT(*) MATCH (x) -/:" + label + "+/-> (y)");

            for (PgxResult result : results)
                if (result.getLong(1).compareTo(0L) != 0)
                    vertexEdgeProperties.put(PrefixPropertyConstant.REACHABILITY_COUNT.toString()
                            .concat(Constants.SEPARATOR_PROPERTY).concat(label), (float) (long) result.getLong(1));
        }
    }


    /**
     * This method extract the label of a property.
     * For example, if we have a property `PERSON_MALE`, this method extracts the label `PERSON`.
     *
     * @param labelAndProperty of the supernode
     *
     * @return the label extracted.
     */
    private String extractLabel(String labelAndProperty) {
        try {
            return labelAndProperty.substring(0, labelAndProperty.indexOf(Constants.SEPARATOR_PROPERTY));
        } catch (IndexOutOfBoundsException e) {
            return labelAndProperty;
        }
    }

    @Override
    public Float computeValue(float totalValue, float occurrences) {

        float value;
        try {
            value = totalValue / occurrences;

            if (Float.isNaN(value))
                value = 0F;
        } catch (ArithmeticException e) {
            value = 0F;
        }

        return value;
    }

    @Override
    public Map<String, Float> getVertexEdgeProperties() {
        return vertexEdgeProperties;
    }
}
