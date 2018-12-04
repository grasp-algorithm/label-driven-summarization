package label.driven.summarization.grouping;

import label.driven.summarization.graph.Session;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.PgxVertex;
import oracle.pgx.api.VertexSet;
import oracle.pgx.api.filter.EdgeFilter;
import oracle.pgx.api.filter.VertexFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 *
 * @author LIRIS
 * @version 1.0
 * @see GroupingImpl#computeGroupings(Session)
 * @since 1.0 5/13/18.
 */
public class GroupingImpl implements Grouping {

    private static final Logger LOG = LogManager.getLogger("GLOBAL");
    private static volatile AtomicInteger idxGrouping = new AtomicInteger(0);
    // Save the grouping with the id of the grouping
    private Map<Integer, Set<Integer>> groupings;
    // Save the correspondence between the grouping & the label
    private Map<Integer, String> correspondenceGroupingLabel;


    public GroupingImpl() {
        groupings = new HashMap<>();
        correspondenceGroupingLabel = new HashMap<>();
    }

    private static Integer getNewGroupingId() {
        return idxGrouping.incrementAndGet();
    }

    private static Integer getCurrentGroupingId() {
        return idxGrouping.get();
    }


    @Override
    public Map<Integer, Set<Integer>> computeGroupings(Session session) {

        List<String> labelFrequents = session.getListOfLabels();
        PgxGraph graph = session.getGraph();

        Collection<Integer> allIDsVertices = new HashSet<>();

        try {
            for (String frequentLabel : labelFrequents) {
                VertexSet<Integer> vertices = graph.filter(new
                        EdgeFilter(String.format("edge.label() = '%s'", frequentLabel))).getVertices();

                Set<Integer> vertexSet = vertices.stream().map(PgxVertex::getId).collect(Collectors.toSet());
                vertexSet.removeAll(allIDsVertices.stream().filter(v -> vertexSet.stream()
                        .anyMatch(v::equals)).collect(Collectors.toSet()));

                if (!vertexSet.isEmpty()) {
                    correspondenceGroupingLabel.put(getNewGroupingId(), frequentLabel);
                    groupings.put(getCurrentGroupingId(), vertexSet);
                    allIDsVertices.addAll(vertexSet);
                }
            }

            /* Here we put in different groupings each isolated vertex (which is not included in the foreach above)*/
            VertexSet<Integer> vertices = graph.getVertices(new
                    VertexFilter("vertex.outDegree() = 0 && vertex.inDegree() = 0"));

            Set<Integer> vertexSet = vertices.stream().map(PgxVertex::getId).collect(Collectors.toSet());
            if (!vertexSet.isEmpty())
                groupings.put(getNewGroupingId(), vertexSet);

        } catch (InterruptedException | ExecutionException e) {
            LOG.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

        allIDsVertices.clear();
        return groupings;
    }

    @Override
    public Map<Integer, Set<Integer>> getGroupings() {
        return groupings;
    }

    @Override
    public Map<Integer, String> getCorrespondenceGroupingLabel() {
        return correspondenceGroupingLabel;
    }
}