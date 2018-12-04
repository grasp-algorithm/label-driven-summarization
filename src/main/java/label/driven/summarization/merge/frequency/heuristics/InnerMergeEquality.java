package label.driven.summarization.merge.frequency.heuristics;

import label.driven.summarization.merge.Constants;
import label.driven.summarization.merge.PrefixPropertyConstant;
import label.driven.summarization.merge.classes.PgxVertexByLabel;
import label.driven.summarization.merge.classes.ReachabilityLabel;
import label.driven.summarization.schema.Schema;
import label.driven.summarization.schema.json.VertexMergeStrategy;
import oracle.pgql.lang.PgqlException;
import oracle.pgx.api.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/27/18.
 */
public class InnerMergeEquality implements MergeHeuristic {


    @Override
    public List<Set<PgxVertex>> mergeSinkVertices(Schema schema, PgxGraph graph) throws ExecutionException, InterruptedException, PgqlException {
        List<Set<PgxVertex>> groupingOfHN = new ArrayList<>();

        Set<ReachabilityLabel> reachabilityLabels = getSNWithFrequentLabel(schema, graph);

        /* For each different reachability-count and frequent label we made a filter and we congregate those nodes
         * in one */
        reachabilityLabels.stream().collect(groupingBy(ReachabilityLabel::getReachabilityPaths,
                groupingBy(ReachabilityLabel::getFrequentLabel))).forEach((reachabilityCount, group) ->
            group.forEach((k,v) -> {
                Set<PgxVertex> set = v.stream().map(ReachabilityLabel::getVertex).collect(Collectors.toSet());
                groupingOfHN.add(set);
            })
        );

        return groupingOfHN;
    }

    Set<ReachabilityLabel> getSNWithFrequentLabel(Schema schema, PgxGraph graph) throws ExecutionException, InterruptedException, PgqlException {
        Set<ReachabilityLabel> reachabilityLabels = new HashSet<>();

        Set<oracle.pgx.api.VertexProperty<?, ?>> properties = graph.getVertexProperties();
        properties.removeIf(prop -> !prop.getName().startsWith(PrefixPropertyConstant.REACHABILITY_COUNT
                .toString()));

        String query = "";

        if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.SOURCE_MERGE))
            query = "SELECT sn MATCH(sn) WHERE out_degree(sn) = 0";
        else if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.TARGET_MERGE))
            query = "SELECT sn MATCH(sn) WHERE in_degree(sn) = 0";

        /* We retrieve all the nodes and their weight with out degree equals to 0 */
        PgqlResultSet results = graph.queryPgql(query);

        /* We save the result in our own structure and we save the different numbers of frequency */
        for (PgxResult result : results) {
            String frequentLabel = "";
            Float reachLabelValue = -1F;

            for (VertexProperty prop : properties) {
                if (((Float) result.getVertex(1).getProperty(prop.getName())) > reachLabelValue) {
                    reachLabelValue = (Float) result.getVertex(1).getProperty(prop.getName());
                    frequentLabel = prop.getName().substring(prop.getName().lastIndexOf(
                            Constants.SEPARATOR_PROPERTY) + 1, prop.getName().length());
                }
            }

            if (reachLabelValue.compareTo(-1F) > 0)
                reachabilityLabels.add(new ReachabilityLabel(result.getVertex(1), frequentLabel, reachLabelValue));
        }

        return reachabilityLabels;
    }

    @Override
    public List<Set<PgxVertex>> mergeSourceAndIntermediateVertices(Schema schema, PgxGraph graph) throws ExecutionException,
            InterruptedException, PgqlException {

        Set<PgxVertexByLabel> labelsByIdVertex = new HashSet<>();

        List<String> properties = graph.getVertexProperties().stream().map(VertexProperty::getName).collect(Collectors.toList());
        properties.removeIf(prop -> !prop.startsWith(PrefixPropertyConstant.REACHABILITY_COUNT
                .toString()));

        String query = "";

        if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.SOURCE_MERGE))
            query = "SELECT sn MATCH (sn) WHERE out_degree(sn) > 0";
        else if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.TARGET_MERGE))
            query = "SELECT sn MATCH (sn) WHERE in_degree(sn) > 0";

        /* We retrieve all the nodes with out degree greater than 0 */
        PgqlResultSet results = graph.queryPgql(query);

        /* We save the result in our own structure and we save the different numbers of frequency */
        for (PgxResult result : results) {
            String frequentLabel = "";
            Float reachLabelValue = -1F;

            for (String prop : properties) {
                if (((Float) result.getVertex(1).getProperty(prop)) > reachLabelValue) {
                    reachLabelValue = (Float) result.getVertex(1).getProperty(prop);
                    frequentLabel = prop.substring(prop.lastIndexOf(
                            Constants.SEPARATOR_PROPERTY) + 1, prop.length());
                }
            }

            PgxVertex<Integer> currentVertex = result.getVertex(1);
            Set<String> outGoingLabelsOfCurrentVertex = new HashSet<>();
            Set<String> inGoingLabelsOfCurrentVertex = new HashSet<>();

            if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.SOURCE_MERGE))
                currentVertex.getOutEdges().forEach(oe -> outGoingLabelsOfCurrentVertex.add(oe.getLabel()));
            if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.TARGET_MERGE))
                currentVertex.getInEdges().forEach(ie -> inGoingLabelsOfCurrentVertex.add(ie.getLabel()));

            if (reachLabelValue.compareTo(-1F) > 0) {
                PgxVertexByLabel pgxVertexByLabel = new PgxVertexByLabel(currentVertex,
                        outGoingLabelsOfCurrentVertex.stream().sorted().collect(Collectors.toList()),
                        inGoingLabelsOfCurrentVertex.stream().sorted().collect(Collectors.toList()),
                        reachLabelValue, frequentLabel);

                /* Here we save the vertex with it set of labels to their treatment bellow */
                labelsByIdVertex.add(pgxVertexByLabel);
            }
        }

        return groupingBySelectedConditionOfMerge(schema, labelsByIdVertex);
    }


    protected List<Set<PgxVertex>> groupingBySelectedConditionOfMerge(Schema schema, Set<PgxVertexByLabel> labelsByIdVertex) {

        List<Set<PgxVertex>> listOfHNs = new ArrayList<>();
        List<Set<PgxVertex>> labelsByOutGoingSetOfLabels = new ArrayList<>();
        List<Set<PgxVertex>> labelsByInGoingSetOfLabels = new ArrayList<>();

        if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.SOURCE_MERGE)) {

            labelsByIdVertex.stream().collect(groupingBy(PgxVertexByLabel::getFrequency,
                    groupingBy(PgxVertexByLabel::getLabel, groupingBy(PgxVertexByLabel::getOutGoingLabels))))
                    .forEach((reachCount, group) ->
                            group.forEach((frequentLabel, list) ->
                                    list.forEach((outGoingList, vertices) ->
                                        labelsByOutGoingSetOfLabels.add(vertices.stream()
                                                .map(PgxVertexByLabel::getVertex)
                                                .collect(Collectors.toSet()))
                                    )));

            listOfHNs.addAll(labelsByOutGoingSetOfLabels);
        } else if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.TARGET_MERGE)) {

            labelsByIdVertex.stream().collect(groupingBy(PgxVertexByLabel::getFrequency,
                    groupingBy(PgxVertexByLabel::getLabel, groupingBy(PgxVertexByLabel::getInGoingLabels))))
                    .forEach((reachCount, group) ->
                            group.forEach((frequentLabel, list) ->
                                    list.forEach((inGoingList, vertices) ->
                                        labelsByInGoingSetOfLabels.add(vertices.stream()
                                                .map(PgxVertexByLabel::getVertex)
                                                .collect(Collectors.toSet()))
                                    )));

            listOfHNs.addAll(labelsByInGoingSetOfLabels);
        }

        return listOfHNs;
    }
}
