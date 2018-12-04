package label.driven.summarization.merge.frequency.heuristics;

import label.driven.summarization.merge.classes.PgxVertexByLabel;
import label.driven.summarization.merge.classes.ReachabilityLabel;
import label.driven.summarization.schema.Schema;
import label.driven.summarization.schema.json.VertexMergeStrategy;
import oracle.pgql.lang.PgqlException;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.PgxVertex;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/27/18.
 */
public class InnerMergeFreqLabel extends InnerMergeEquality {
    @Override
    public List<Set<PgxVertex>> mergeSinkVertices(Schema schema, PgxGraph graph) throws ExecutionException, InterruptedException, PgqlException {
        List<Set<PgxVertex>> groupingOfHN = new ArrayList<>();

        Set<ReachabilityLabel> reachabilityLabels = getSNWithFrequentLabel(schema, graph);

        /* For each different reachability-count and frequent label we made a filter and we congregate those nodes
         * in one */
        reachabilityLabels.stream().collect(groupingBy(ReachabilityLabel::getFrequentLabel)).forEach((freqLabel, group) -> {
            Set<PgxVertex> set = group.stream().map(ReachabilityLabel::getVertex).collect(Collectors.toSet());
            groupingOfHN.add(set);
        });

        return groupingOfHN;
    }


    @Override
    protected List<Set<PgxVertex>> groupingBySelectedConditionOfMerge(Schema schema, Set<PgxVertexByLabel> labelsByIdVertex) {

        List<Set<PgxVertex>> listOfHNs = new ArrayList<>();
        List<Set<PgxVertex>> labelsByOutGoingSetOfLabels = new ArrayList<>();
        List<Set<PgxVertex>> labelsByInGoingSetOfLabels = new ArrayList<>();

        if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.SOURCE_MERGE)) {

            labelsByIdVertex.stream().collect(groupingBy(PgxVertexByLabel::getLabel, groupingBy(PgxVertexByLabel::getOutGoingLabels)))
                    .forEach((freqLabel, group) ->
                            group.forEach((outGoingList, vertices) ->
                                labelsByOutGoingSetOfLabels.add(vertices.stream()
                                        .map(PgxVertexByLabel::getVertex)
                                        .collect(Collectors.toSet()))
                            ));

            listOfHNs.addAll(labelsByOutGoingSetOfLabels);
        }

        if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.TARGET_MERGE)) {
            labelsByIdVertex.stream()
                    .collect(groupingBy(PgxVertexByLabel::getLabel, groupingBy(PgxVertexByLabel::getInGoingLabels)))
                    .forEach((reachCount, group) ->
                            group.forEach((inComingList, vertices) ->
                                            labelsByInGoingSetOfLabels.add(vertices.stream()
                                                    .map(PgxVertexByLabel::getVertex)
                                                    .collect(Collectors.toSet()))
                                         ));

            if (!labelsByOutGoingSetOfLabels.isEmpty()) {
                listOfHNs.retainAll(labelsByInGoingSetOfLabels);
            } else {
                listOfHNs.addAll(labelsByInGoingSetOfLabels);
            }
        }

        return listOfHNs;
    }


}
