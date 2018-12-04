package label.driven.summarization.merge.frequency;

import label.driven.summarization.graph.GraphUtils;
import label.driven.summarization.merge.Constants;
import label.driven.summarization.merge.PrefixPropertyConstant;
import label.driven.summarization.merge.evaluation.histogram.properties.PairDirectionOption;
import label.driven.summarization.schema.Schema;
import label.driven.summarization.schema.json.PropertyMergeStrategy;
import oracle.pgql.lang.PgqlException;
import oracle.pgx.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class is responsible of the merge of the properties to the hyper-node.
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/24/18.
 */
class MergePropertyStrategy {

    private static final Logger LOG = LogManager.getLogger("GLOBAL");
    private Map<String, Float> newProperties;
    private PgxGraph graph;

    MergePropertyStrategy(PgxGraph graph) {
        this.newProperties = new HashMap<>();
        this.graph = graph;
    }

    /**
     * Strategy to merge the properties of the vertices and compute the inner edges of each hyper-node.
     *
     * @param schema           the schema of the dataset
     * @param vertexProperties the set of all the properties
     * @param pgxVertices      the list of vertices that will be merged.
     *
     * @return the list of new properties of the hyper-node
     */
    Map<String, Float> mergeProperty(Schema schema, Set<String> vertexProperties, Set<PgxVertex> pgxVertices) {
        Map<String, Float> auxProperties = new HashMap<>();
        Map<String, Float> globalValues = new HashMap<>();
        Map<String, Float> labelsOccurrences = new HashMap<>();
        Map<String, Float> propertyOccurrences = new HashMap<>();
        Map<String, Float> predicatesOccurrences = new HashMap<>();
        Set<String> pendingProperties = new HashSet<>();

        /* We remove the property grouping, because we start to merge different groupings */
        vertexProperties.removeIf(prop -> prop.equals(PrefixPropertyConstant.GROUPING.toString()));
        /* We remove the traversal frontiers properties because they'll receive another treatment
         * @see MergePropertyStrategy#computeFrontierTraversalProperties  */
        vertexProperties.removeIf(prop -> prop.startsWith(PrefixPropertyConstant.TRAVERSAL_FRONTIERS.toString()));


        pgxVertices.forEach(sn -> {
            try {

                Map<String, Float> localLabelsCount = new HashMap<>();
                classifyProperties(sn, vertexProperties, globalValues, labelsOccurrences, localLabelsCount,
                        pendingProperties, predicatesOccurrences);

                /* we traverse again the list of vertices, because now we need to compute the percentage of the pending
                 * properties by the number of occurrences of each label*/
                pendingProperties.forEach(pp -> {
                    /* We check that the property shouldn't be taken into account */
                    if (schema.getAllVertexProperties().stream().noneMatch(prop -> prop.getName().equals(pp) &&
                            prop.getMergeStrategy().equals(PropertyMergeStrategy.IGNORE)))
                        try {
                            AtomicReference<String> label = new AtomicReference<>();

                            /* We retrieve the label of the property, attention to the fact that the properties have the
                             * following format: LABEL_PROPERTY */
                            labelsOccurrences.keySet().stream().filter(pp.substring(0, pp.indexOf(Constants
                                    .SEPARATOR_PROPERTY))::equals).findAny().ifPresent(label::set);

                            /* We compute the total number of this properties taken into account the number of occurrence
                             * of `this` label, i.e. if we have a property Person_Female, we take the percentage and
                             * we multiply to the number of `Persons` because, in the same node we can found different
                             * labels */
                            propertyOccurrences.put(pp, propertyOccurrences.getOrDefault(pp, 0F) +
                                    (Float) sn.getProperty(pp) * localLabelsCount.get(label.get()));
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

        /* Once we have the sum, now we're re-compute the percentage by the corresponding label */
        propertyOccurrences.forEach((k, v) -> {
            /* We retrieve the label of the property, attention to the fact that the properties have the
             * following format: LABEL_PROPERTY */
            AtomicReference<String> label = new AtomicReference<>();
            labelsOccurrences.keySet().stream().filter(k.substring(0, k.indexOf(Constants
                    .SEPARATOR_PROPERTY))::equals).findAny().ifPresent(label::set);

            /* We compute the division (see previous block) */
            propertyOccurrences.put(k, propertyOccurrences.getOrDefault(k, 0F) /
                    labelsOccurrences.get(label.get()));
        });


        /* After the previous step, we transform the number of occurrences of the labels by the percentage
         * (i.e. divided by the number of nodes inside this 'hyper-node') */
        labelsOccurrences.forEach((k, v) ->
                auxProperties.put(k, v / globalValues.get(PrefixPropertyConstant.NODE_WEIGHT_EVALUATION.toString())));

        predicatesOccurrences.entrySet().stream().filter(entry -> entry.getValue() > 0F).forEach(entry ->
                auxProperties.put(entry.getKey(), entry.getValue() /
                        globalValues.get(PrefixPropertyConstant.NUMBER_OF_INNER_EDGES.toString())));

        /* Once we made all the calculations, we put all the properties into the map */
        newProperties.putAll(globalValues);
        newProperties.putAll(auxProperties);
        newProperties.putAll(propertyOccurrences);

        return newProperties;
    }

    /**
     * Classify the properties according to the type of property (COUNT, PERCENTAGE, LABEL, etc..)
     *
     * @param sn                the current super-node
     * @param vertexProperties  the list of new properties of the hyper-node
     * @param globalValues      global values which doesn't need any treatment
     * @param labelsOccurrences the number of occurrences of each label on the super-node.
     * @param localLabelsCount  the count of the label of the current vertex, used later to multiply with the percentage
     * @param pendingProperties properties that can not be computed without have the total number of occurrences of each
     *                          label
     *
     * @throws ExecutionException   if the property can not be accessed.
     * @throws InterruptedException if the property can not be accessed.
     */
    private void classifyProperties(PgxVertex sn, Set<String> vertexProperties, Map<String, Float> globalValues,
                                    Map<String, Float> labelsOccurrences, Map<String, Float> localLabelsCount,
                                    Set<String> pendingProperties, Map<String, Float> predicatesOccurrences) throws ExecutionException, InterruptedException {

        float nodeWeightSN = (Float) sn.getProperty(PrefixPropertyConstant.NODE_WEIGHT_EVALUATION.toString());
        float numberOfInnerEdgesSN = (Float) sn.getProperty(PrefixPropertyConstant.NUMBER_OF_INNER_EDGES.toString());

        vertexProperties.forEach(vp -> {
            try {
                /* if the property is count or the number of Edges, with sum the occurrences */
                if (vp.equals(PrefixPropertyConstant.NODE_WEIGHT_EVALUATION.toString()) ||
                        vp.equals(PrefixPropertyConstant.NUMBER_OF_INNER_EDGES.toString()) ||
                        vp.startsWith("REACH") || vp.startsWith("PARTICIPATION_LABEL"))
                    globalValues.put(vp, globalValues.getOrDefault(vp, 0F) + ((Float) sn.getProperty(vp)));

                    /* otherwise, we accumulate the number of occurrences of labels in the list of vertices */
                else if (vp.startsWith(PrefixPropertyConstant.LABEL.toString())) {
                    String label = extractLabel(vp);

                    /* Here we accumulate the NODE_WEIGHT_EVALUATION by the percentage of the current label (i.e. if we have a
                     * (LABEL_PERSON, 0.5) and (NODE_WEIGHT_EVALUATION, 6), it means that we're going to accumulate 3 (= 6*0.5)) */
                    labelsOccurrences.put(label, labelsOccurrences.getOrDefault(label, 0F) +
                            (Float) sn.getProperty(vp) * nodeWeightSN);
                    localLabelsCount.put(label, (Float) sn.getProperty(vp) * nodeWeightSN);

                    /* Similar to the labels, in the inner-edges, we have percentages, so, will need to compute the
                     * scalar value by predicate */
                } else if (vp.startsWith(PrefixPropertyConstant.PERCENTAGE.toString())) {
                    predicatesOccurrences.put(vp, predicatesOccurrences.getOrDefault(vp, 0F) +
                            (numberOfInnerEdgesSN * (Float) sn.getProperty(vp)));

                    /* Or, we save the properties that we can not compute yet because we need the division by
                     * the number of labels (previous block) e.g. properties like (PERSON_MALE, 0.4) */
                } else if ((Float) sn.getProperty(vp) != 0F) {
                    pendingProperties.add(vp);


                }
            } catch (ExecutionException | InterruptedException e) {
                LOG.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        });
    }

    Map<String, Float> computeTraversalFrontiers(Schema schema, Set<PgxVertex> pgxVertices) {
        if (schema.isComputeConcatenationProperties()) {
            try {
                GraphChangeSet<Integer> changeSet = graph.createChangeSet();
                pgxVertices.forEach(v -> changeSet.updateVertex((Integer) v.getId())
                        .setProperty(Constants.BLOCKING_PROPERTY, true));

                PgxGraph updatedGraph = changeSet.build();
                String query = "SELECT x, label(e1), label(e2), COUNT(*) MATCH %s WHERE x.BLOCKED = True AND " +
                        "y.BLOCKED = False AND z.BLOCKED = False AND label(e1) <= label(e2) GROUP BY x, label(e1), " +
                        "label(e2) ";

                if (schema.isAllLabelsAllowed()) {
                    return computeFourCombinationsForTraversalFrontiers(updatedGraph, query, null, null);
                } else {

                    Map<String, Float> frontierTraversalProperties = new HashMap<>();
                    PgxGraph filteredGraph = GraphUtils.getFilteredGraph(schema, updatedGraph);
                    for (String labels : schema.getAllowedLabels()) {
                        String[] pair = labels.split(":");
                        frontierTraversalProperties.putAll(computeFourCombinationsForTraversalFrontiers(filteredGraph, query, pair[0], pair[1]));
                    }

                    return frontierTraversalProperties;
                }

            } catch (ExecutionException | InterruptedException e) {
                LOG.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }

        }

        return Collections.emptyMap();
    }

    private Map<String, Float> computeFourCombinationsForTraversalFrontiers(PgxGraph graph, String query, String label1, String label2) {

        Map<String, Float> frontierTraversalProperties = new HashMap<>();

        String edgeVar1 = "e1";
        String edgeVar2 = "e2";

        if (label1 != null) edgeVar1 = edgeVar1.concat(":").concat(label1);
        if (label2 != null) edgeVar2 = edgeVar2.concat(":").concat(label2);

        frontierTraversalProperties.putAll(
                computeFrontierTraversalByHN(graph, String.format(query, String.format("(y) -[%s]-> (x) <-[%s]- (z)",
                        edgeVar1, edgeVar2)), PairDirectionOption.IN_IN));

        frontierTraversalProperties.putAll(
                computeFrontierTraversalByHN(graph, String.format(query, String.format("(y) -[%s]-> (x) -[%s]-> (z)",
                        edgeVar1, edgeVar2)), PairDirectionOption.IN_OUT));


        frontierTraversalProperties.putAll(
                computeFrontierTraversalByHN(graph, String.format(query, String.format("(y) <-[%s]- (x) <-[%s]- (z)",
                        edgeVar1, edgeVar2)), PairDirectionOption.OUT_IN));

        frontierTraversalProperties.putAll(
                computeFrontierTraversalByHN(graph, String.format(query, String.format("(y) <-[%s]- (x) -[%s]-> (z)",
                        edgeVar1, edgeVar2)), PairDirectionOption.OUT_OUT));

        return frontierTraversalProperties;
    }

    //"SELECT x, label(e1), label(e2) MATCH (y) -[e1]-> (x) <-[e2]- (z) " +
    //                    "WHERE x.BLOCKED = True AND y.BLOCKED = False AND z.BLOCKED = False GROUP BY label(e1), label(e2) " +
    private Map<String, Float> computeFrontierTraversalByHN(PgxGraph graph, String query,
                                                            PairDirectionOption direction) {

        Map<String, Float> frontierTraversalProperties = new HashMap<>();

        try {
            PgqlResultSet results = graph.queryPgql(query);

            for (PgxResult result : results) {
                PgxVertex currentVertex = result.getVertex(1);

                String property = PrefixPropertyConstant.TRAVERSAL_FRONTIERS.toString()
                        .concat(direction.getDescription())
                        .concat(Constants.SEPARATOR_PROPERTY).concat(result.getString(2))
                        .concat(Constants.SEPARATOR_PROPERTY).concat(result.getString(3));

                if (graph.getVertexProperties().stream().noneMatch(prop -> prop.getName().equals(property)))
                    continue;

                if ((Float) currentVertex.getProperty(property) > 0F)
                    frontierTraversalProperties.put(property,
                            frontierTraversalProperties.getOrDefault(property, 0F) + (Float) currentVertex.getProperty(property));
            }
        } catch (PgqlException | ExecutionException | InterruptedException e) {
            LOG.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

        return frontierTraversalProperties;
    }


    /**
     * Given a property, this method extract the label.
     * i.e. PERSON_MALE => PERSON.
     *
     * @param vp vertex property
     *
     * @return the label
     */
    private String extractLabel(String vp) {
        return vp.replace(PrefixPropertyConstant.LABEL.toString()
                .concat(Constants.SEPARATOR_PROPERTY), "");
    }

    /**
     * @return the list of properties for the hyper-node
     */
    Map<String, Float> getNewProperties() {
        return newProperties;
    }
}
