package label.driven.summarization.merge.evaluation.histogram.properties;

import label.driven.summarization.merge.Constants;
import label.driven.summarization.merge.PrefixPropertyConstant;
import label.driven.summarization.schema.Schema;
import oracle.pgql.lang.PgqlException;
import oracle.pgx.api.PgqlResultSet;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.PgxResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 7/12/18.
 */
public class TraversalFrontierProperty {

    private TraversalFrontierProperty() {
        throw new UnsupportedOperationException("This class can not be instantiated.");
    }

    public static Map<String, Float> computeFrontierTraversalsByLabel(Schema schema, PgxGraph graph) throws ExecutionException, InterruptedException, PgqlException {

        /* the variable x represents the frontier node, the variables y and z are external nodes (could be from another
         * super node) we add the condition of `label(e1) <= label(e2)` in order to preserve the lexicographic order */
        String query = "SELECT label(e1), label(e2), COUNT(*) MATCH %s WHERE z.BLOCKED = False AND x.BLOCKED = True AND " +
                "y.BLOCKED = False AND e1 != e2 GROUP BY label(e1), label(e2)";

        if (schema.isComputeConcatenationProperties()) {

            if (schema.isAllLabelsAllowed()) {
//                PgxGraph filteredGraph = GraphUtils.getFilteredGraph(schema, graph);
                return computeTheForCombinationsForTraversalFrontiers(graph, query, null, null);

            } else {

                Map<String, Float> vertexEdgeProperties = new HashMap<>();

//                for (String labels : schema.getAllowedLabels()) {
//                    String[] pair = labels.split(":");

//                    PgxGraph filteredGraph = GraphUtils.getFilteredGraph(schema, graph);
//                    if (filteredGraph.getVertexProperty("BLOCKED") != null)
                for (String labels : schema.getAllowedLabels()) {
                    String[] pair = labels.split(":");

                    //we respect the order to avoid the computation of duplicata
                    if (pair[0].compareTo(pair[1]) > 0)
                        vertexEdgeProperties.putAll(computeTheForCombinationsForTraversalFrontiers(graph, query,
                                pair[0], pair[1]));
                    else
                        vertexEdgeProperties.putAll(computeTheForCombinationsForTraversalFrontiers(graph, query,
                                pair[1], pair[0]));

                }
//                    filteredGraph.destroy();
//                }

                return vertexEdgeProperties;
            }
        }

        return Collections.emptyMap();
    }

    private static Map<String, Float> computeTheForCombinationsForTraversalFrontiers(PgxGraph graph, String query, String label1, String label2)
            throws InterruptedException, ExecutionException, PgqlException {

        Map<String, Float> vertexEdgeProperties = new HashMap<>();

        String edgeVar1 = "e2"; // cross-edge
        String edgeVar2 = "e1"; // cross-edge

        if (label1 != null) edgeVar1 = edgeVar1.concat(":").concat(label1);
        if (label2 != null) edgeVar2 = edgeVar2.concat(":").concat(label2);

        /* frontier traversals out out */
        vertexEdgeProperties.putAll(computeExistenceOfFrontierTraversals(graph, String.format(query, String.format("(z) <-[%s]- (x) -[%s]-> (y)",
                edgeVar1, edgeVar2)), PairDirectionOption.OUT_OUT.getDescription()));

        /* frontier traversals in in */
        vertexEdgeProperties.putAll(computeExistenceOfFrontierTraversals(graph, String.format(query, String.format("(z) -[%s]-> (x) <-[%s]- (y)",
                edgeVar1, edgeVar2)), PairDirectionOption.IN_IN.getDescription()));

        /* frontier traversals out in */
        vertexEdgeProperties.putAll(computeExistenceOfFrontierTraversals(graph, String.format(query, String.format("(z) <-[%s]- (x) <-[%s]- (y)",
                edgeVar1, edgeVar2)), PairDirectionOption.IN_OUT.getDescription()));

        /*frontier traversals in  our*/
        vertexEdgeProperties.putAll(computeExistenceOfFrontierTraversals(graph, String.format(query, String.format("(z) -[%s]-> (x) -[%s]-> (y)",
                edgeVar1, edgeVar2)), PairDirectionOption.OUT_IN.getDescription()));

        return vertexEdgeProperties;
    }


    private static Map<String, Float> computeExistenceOfFrontierTraversals(PgxGraph graph, String query, String orientation)
            throws ExecutionException, InterruptedException, PgqlException {

        Map<String, Float> vertexEdgeProperties = new HashMap<>();

        PgqlResultSet results = graph.queryPgql(query);
        String property = PrefixPropertyConstant.TRAVERSAL_FRONTIERS.toString().concat(orientation);


        for (PgxResult result : results) {
            vertexEdgeProperties.put(property.concat(Constants.SEPARATOR_PROPERTY
                    .concat(result.getString(1))
                    .concat(Constants.SEPARATOR_PROPERTY)
                    .concat(result.getString(2))), (float) (long) result.getLong(3));
        }


        return vertexEdgeProperties;
    }

}
