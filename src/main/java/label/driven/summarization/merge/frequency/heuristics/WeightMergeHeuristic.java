package label.driven.summarization.merge.frequency.heuristics;

import label.driven.summarization.merge.PrefixPropertyConstant;
import label.driven.summarization.merge.classes.PgxVertexByLabel;
import label.driven.summarization.merge.classes.Functions;
import label.driven.summarization.schema.Schema;
import label.driven.summarization.schema.json.VertexMergeStrategy;
import oracle.pgql.lang.PgqlException;
import oracle.pgx.api.PgqlResultSet;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.PgxResult;
import oracle.pgx.api.PgxVertex;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/27/18.
 */
public class WeightMergeHeuristic implements MergeHeuristic {

    @Override
    public List<Set<PgxVertex>> mergeSinkVertices(Schema schema, PgxGraph graph) throws ExecutionException, InterruptedException,
            PgqlException {
        List<Set<PgxVertex>> groupingOfHN = new ArrayList<>();

        Set<Integer> frequencyOccurrences = new HashSet<>();
        Map<PgxVertex<Integer>, Integer> nodesWithNoneOutDegree = new HashMap<>();

        String query = String.format("SELECT sn, sn.%1$s  MATCH(sn) WHERE out_degree(sn) = 0 ORDER BY sn.%1$s",
                PrefixPropertyConstant.NODE_WEIGHT_EVALUATION.toString());


        /* We retrieve all the nodes and their weight with out degree equals to 0 */
        PgqlResultSet results = graph.queryPgql(query);
        /* We save the result in our own structure and we save the different numbers of frequency */
        for (PgxResult result : results) {
            nodesWithNoneOutDegree.put(result.getVertex(1), (int) (float) result.getFloat(2));
            frequencyOccurrences.add((int) (float) result.getFloat(2));
        }

        /* For each different node-weight we made a filter and we congregate those nodes in one */
        frequencyOccurrences.forEach(nodeWeight -> {
            Set<PgxVertex> set = nodesWithNoneOutDegree.entrySet().stream()
                    .filter(m -> m.getValue().equals(nodeWeight)).map(Map.Entry::getKey).collect(Collectors.toSet());

            groupingOfHN.add(set);
        });

        return groupingOfHN;
    }

    @Override
    public List<Set<PgxVertex>> mergeSourceAndIntermediateVertices(Schema schema, PgxGraph graph) throws ExecutionException, InterruptedException, PgqlException {
        Set<PgxVertexByLabel> labelsByIdVertex = new HashSet<>();

        /* First we compute the set of different weight on the edge (i.e. the different values of the
            property NODE_WEIGHT_EVALUATION or REACH_NUMBER_OF_INNER_PATHS)*/
        Set<Integer> nodeWeights = new HashSet<>();
        Set<Float> newNodeWeights = new HashSet<>();

        PgqlResultSet results = graph.queryPgql(String.format("SELECT n.%s MATCH (n) WHERE out_degree(n) > 0",
                PrefixPropertyConstant.NODE_WEIGHT_EVALUATION.toString()));

        for (PgxResult result : results)
            nodeWeights.add((int) (float) result.getFloat(1));

        /* We sort the list ascending */
        List<Integer> nodeWeightsSorted = nodeWeights.stream().sorted()
                .collect(Collectors.toList());

        AtomicInteger sumOfFrequencyAndEps = new AtomicInteger(0);

        /* For each different value of NODE_WEIGHT_EVALUATION or REACH_NUMBER_OF_INNER_PATHS we filter all the nodes
         * with this value on their property and we retrieve all the labels of each vertex and we saved on the
         * `labelsByIdVertex` structure */
        for (Integer nodeWeight : nodeWeightsSorted) {

            /* We check the condition of the epsilon parameter */
            if (nodeWeight > sumOfFrequencyAndEps.get()) {

                /* We query to know the set of vertices with the given NODE_WEIGHT_EVALUATION and we compute the
                 * set of labels on the outgoing edges */
                PgqlResultSet resultSet = graph.queryPgql(builtQueryByRangeBySchemaConfig(schema, nodeWeight));

                for (PgxResult result : resultSet) {
                    PgxVertex<Integer> currentVertex = result.getVertex(1);

                    Set<String> outGoingLabelsOfCurrentVertex = new HashSet<>();
                    Set<String> inGoingLabelsOfCurrentVertex = new HashSet<>();

                    if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.SOURCE_MERGE))
                        currentVertex.getOutEdges().forEach(oe -> outGoingLabelsOfCurrentVertex.add(oe.getLabel()));
                    if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.TARGET_MERGE))
                        currentVertex.getInEdges().forEach(ie -> inGoingLabelsOfCurrentVertex.add(ie.getLabel()));

                    PgxVertexByLabel pgxVertexByLabel = new PgxVertexByLabel(currentVertex,
                            outGoingLabelsOfCurrentVertex.stream().sorted().collect(Collectors.toList()),
                            inGoingLabelsOfCurrentVertex.stream().sorted().collect(Collectors.toList()),
                            Functions.avg(nodeWeight, nodeWeight + schema.getConfig().getEpsilon()));

                    /* Here we use the average only because we want to have the same value for all
                     * the vertices in the same range, but we're not computing this value */
                    newNodeWeights.add(Functions.avg(nodeWeight, nodeWeight + schema.getConfig().getEpsilon()));

                    /* Here we save the vertex with it set of labels to their treatment bellow */
                    labelsByIdVertex.add(pgxVertexByLabel);
                }

                sumOfFrequencyAndEps.set(nodeWeight + schema.getConfig().getEpsilon());
            }

        }

        /* For each node_weight, we made a sort of map-reduce model, where in the map we made a projection
         * of the set of labels and the vertex id, and in the reduce function, we merge the nodes with the same
         * label set. */

        return groupingBySelectedConditionOfMerge(schema, labelsByIdVertex, newNodeWeights);
    }


    /**
     * @param schema
     * @param labelsByIdVertex
     * @param newNodeWeights
     *
     * @return
     */
    private List<Set<PgxVertex>> groupingBySelectedConditionOfMerge(Schema schema, Set<PgxVertexByLabel> labelsByIdVertex,
                                                                    Set<Float> newNodeWeights) {

        List<Set<PgxVertex>> listOfHNs = new ArrayList<>();
        Map<List<String>, Set<PgxVertex>> labelsByOutGoingSetOfLabels = null;
        Map<List<String>, Set<PgxVertex>> labelsByInGoingSetOfLabels;

        for (Float nodeWeight : newNodeWeights) {

            if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.SOURCE_MERGE)) {

                labelsByOutGoingSetOfLabels = labelsByIdVertex.stream()
                        .filter(l -> l.getFrequency().equals(nodeWeight))
                        .collect(Collectors.toMap(PgxVertexByLabel::getOutGoingLabels, item ->
                                        Collections.singleton(item.getVertex()),
                                (a, b) -> {
                                    Set<PgxVertex> mergeVertices = new HashSet<>(a);
                                    mergeVertices.addAll(b);
                                    return mergeVertices;
                                }));

                listOfHNs.addAll(labelsByOutGoingSetOfLabels.entrySet()
                        .stream()
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList()));
            }

            if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.TARGET_MERGE)) {
                labelsByInGoingSetOfLabels = labelsByIdVertex.stream()
                        .filter(l -> l.getFrequency().equals(nodeWeight))
                        .collect(Collectors.toMap(PgxVertexByLabel::getInGoingLabels, item ->
                                        Collections.singleton(item.getVertex()),
                                (a, b) -> {
                                    Set<PgxVertex> mergeVertices = new HashSet<>(a);
                                    mergeVertices.addAll(b);
                                    return mergeVertices;
                                }));

                if (labelsByOutGoingSetOfLabels != null) {
                    List<Set<PgxVertex>> aux = labelsByInGoingSetOfLabels.entrySet().stream()
                            .map(Map.Entry::getValue).collect(Collectors.toList());
                    listOfHNs.retainAll(aux);
                } else
                    listOfHNs.addAll(labelsByInGoingSetOfLabels.entrySet().stream().map(Map.Entry::getValue)
                            .collect(Collectors.toList()));
            }

        }

        return listOfHNs;
    }

}
