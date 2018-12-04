package label.driven.summarization.merge.evaluation.histogram.properties;

import label.driven.summarization.merge.Constants;
import label.driven.summarization.merge.PrefixPropertyConstant;
import label.driven.summarization.schema.Schema;
import oracle.pgql.lang.PgqlException;
import oracle.pgx.api.PgqlResultSet;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.PgxResult;
import oracle.pgx.api.PgxVertex;
import oracle.pgx.api.filter.EdgeFilter;
import oracle.pgx.api.filter.VertexFilter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 7/12/18.
 */
public class LabelParticipationProperty {
    private static final Logger LOG = LogManager.getLogger("GLOBAL");

    interface CrossAndInnerEdge {

        Integer getFrontierNodeId();

        String getLabelOnCrossEdge();

        Integer getInnerNodeId();

        String getLabelOnInnerEdge();
    }

    private LabelParticipationProperty() {
        throw new UnsupportedOperationException("This class can not be instantiated.");
    }

    public static Map<String, Float> computeParticipationLevelByLabel(Schema schema, PgxGraph graph) throws ExecutionException, InterruptedException,
            PgqlException {

        /* the variable `x` represents the frontier node, the variable `z` is a inner-node (could be another frontier
         * node) and the variable `y` is a external node in another SN */
        String query = "SELECT x, label(e1), label(e2), z MATCH %s WHERE z.BLOCKED = True AND x.BLOCKED = True AND " +
                "y.BLOCKED = False";

        if (schema.isComputeConcatenationProperties()) {

            if (schema.isAllLabelsAllowed()) {

//                PgxGraph filteredGraph = GraphUtils.getFilteredGraph(schema, graph);
                return computeTheFourCombinationsForLabelParticipation(graph, query, null, null);

            } else {

                Map<String, Float> vertexEdgeProperties = new HashMap<>();
                for (String labels : schema.getAllowedLabels()) {
                    String[] pair = labels.split(":");
//                    PgxGraph filteredGraph = GraphUtils.getFilteredGraph(Arrays.asList(pair), graph);
                    vertexEdgeProperties.putAll(computeTheFourCombinationsForLabelParticipation(graph, query,
                            pair[0], pair[1]));

                    if (!pair[0].equals(pair[1]))
                        vertexEdgeProperties.putAll(computeTheFourCombinationsForLabelParticipation(graph, query,
                                pair[1], pair[0]));
                }

                return vertexEdgeProperties;
            }
        }

        return Collections.emptyMap();
    }


    private static Map<String, Float> computeTheFourCombinationsForLabelParticipation(PgxGraph graph, String query, String labelOnInner,
                                                                                      String labelOnCrossEdge) throws InterruptedException,
            ExecutionException, PgqlException {
        Map<String, Float> vertexEdgeProperties = new HashMap<>();

        String edgeVar1 = "e2"; // inner-edge
        String edgeVar2 = "e1"; // cross-edge

        if (labelOnInner != null) edgeVar1 = edgeVar1.concat(":").concat(labelOnInner);
        if (labelOnCrossEdge != null) edgeVar2 = edgeVar2.concat(":").concat(labelOnCrossEdge);

        /* frontier nodes out out */
        vertexEdgeProperties.putAll(computeInOutDegreeByFrontierNode(graph, String.format(query,
                String.format("(z) <-[%s]- (x) -[%s]-> (y)", edgeVar1, edgeVar2)),
                PairDirectionOption.OUT_OUT.getDescription()));

        /* frontier nodes in in */
        vertexEdgeProperties.putAll(computeInOutDegreeByFrontierNode(graph, String.format(query,
                String.format("(z) -[%s]-> (x) <-[%s]- (y)", edgeVar1, edgeVar2)),
                PairDirectionOption.IN_IN.getDescription()));

        /* frontier nodes in out */
        vertexEdgeProperties.putAll(computeInOutDegreeByFrontierNode(graph, String.format(query,
                String.format("(z) <-[%s]- (x) <-[%s]- (y)", edgeVar1, edgeVar2)),
                PairDirectionOption.IN_OUT.getDescription()));

        /* frontier nodes out in */
        vertexEdgeProperties.putAll(computeInOutDegreeByFrontierNode(graph, String.format(query,
                String.format("(z) -[%s]-> (x) -[%s]-> (y)", edgeVar1, edgeVar2)),
                PairDirectionOption.OUT_IN.getDescription()));

        return vertexEdgeProperties;

    }


    private static Map<String, Float> computeInOutDegreeByFrontierNode(PgxGraph graph, String query, String propertyLabelParticipation)
            throws ExecutionException, InterruptedException, PgqlException {

        Map<String, Float> vertexEdgeProperties = new HashMap<>();
        Set<CrossAndInnerEdge> crossAndInnerEdges = new HashSet<>();
        PgqlResultSet results = graph.queryPgql(query);

        for (PgxResult result : results) {

            PgxVertex frontierNode = result.getVertex(1);
            Integer frontierNodeId = (Integer) frontierNode.getId();
            PgxVertex innerNode = result.getVertex(4);
            Integer innerNodeId = (Integer) innerNode.getId();
            String labelOnCrossEdge = result.getString(2);
            String labelOnInnerEdge = result.getString(3);

            CrossAndInnerEdge crossAndInnerEdge = new CrossAndInnerEdge() {
                @Override
                public Integer getFrontierNodeId() {
                    return frontierNodeId;
                }

                @Override
                public String getLabelOnCrossEdge() {
                    return labelOnCrossEdge;
                }

                @Override
                public Integer getInnerNodeId() {
                    return innerNodeId;
                }

                @Override
                public String getLabelOnInnerEdge() {
                    return labelOnInnerEdge;
                }


                @Override
                public int hashCode() {
                    return new HashCodeBuilder(17, 37)
                            .append(getFrontierNodeId())
                            .append(getInnerNodeId())
                            .append(getLabelOnCrossEdge())
                            .append(getLabelOnInnerEdge())
                            .toHashCode();
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;

                    if (o == null || getClass() != o.getClass()) return false;

                    CrossAndInnerEdge that = (CrossAndInnerEdge) o;

                    return new EqualsBuilder()
                            .append(this.getFrontierNodeId(), that.getFrontierNodeId())
                            .append(this.getInnerNodeId(), that.getInnerNodeId())
                            .append(this.getLabelOnCrossEdge(), that.getLabelOnCrossEdge())
                            .append(this.getLabelOnInnerEdge(), that.getLabelOnInnerEdge())
                            .isEquals();
                }
            };

            crossAndInnerEdges.add(crossAndInnerEdge);
        }

        String property = PrefixPropertyConstant.PARTICIPATION_LABEL.toString().concat(propertyLabelParticipation);
        crossAndInnerEdges.stream().collect(Collectors.groupingBy(CrossAndInnerEdge::getLabelOnCrossEdge,
                Collectors.groupingBy(CrossAndInnerEdge::getLabelOnInnerEdge))).forEach((labelOnCrossEdge, list) ->
                list.forEach((labelOnInnerEdge, set) -> {

                    float frontierNodes = 0F;
                    try {
                        frontierNodes = getNumberOfFrontierNodesByLabel(graph, labelOnCrossEdge, propertyLabelParticipation);
                    } catch (ExecutionException | InterruptedException e) {
                        LOG.error(e.getMessage(), e);
                        Thread.currentThread().interrupt();
                    }

                    vertexEdgeProperties.put(property.concat(Constants.SEPARATOR_PROPERTY
                                    .concat(labelOnCrossEdge)
                                    .concat(Constants.SEPARATOR_PROPERTY)
                                    .concat(labelOnInnerEdge)),
                            set.size() / frontierNodes);
                }));


        return vertexEdgeProperties;
    }


    private static float getNumberOfFrontierNodesByLabel(PgxGraph graph, String labelOnCrossEdge, String property) throws ExecutionException, InterruptedException {

        String filter;
        switch (property) {
            case "_IN_IN":
            case "_IN_OUT":
                filter = "!(src.BLOCKED) && dst.BLOCKED";
                break;
            case "_OUT_OUT":
            case "_OUT_IN":
                filter = "src.BLOCKED && !(dst.BLOCKED)";
                break;
            default:
                filter = "";
        }

        return (float) graph.filter(new EdgeFilter(String.format("edge.label() = '%s' && %s", labelOnCrossEdge, filter)))
                .filter(new VertexFilter("vertex.BLOCKED")).getNumVertices();
    }
}
