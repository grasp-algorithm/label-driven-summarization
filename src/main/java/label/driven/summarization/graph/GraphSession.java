package label.driven.summarization.graph;

import label.driven.summarization.merge.common.SuperNode;
import label.driven.summarization.merge.PrefixPropertyConstant;
import label.driven.summarization.schema.Schema;
import oracle.pgx.api.*;
import oracle.pgx.api.filter.EdgeFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * This class preserve the PGX Session.
 * Here the methods to manipulate the input graph.
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/13/18.
 *
 */
public class GraphSession implements Session {
    public static final String LABEL_PREFIX = "l";
    private static final Logger LOG = LogManager.getLogger("GLOBAL");
    private static final String SESSION_NAME = "my-session";

    private PgxSession session;
    private PgxGraph graph;
    private Map<String, Integer> distinctLabels;

    @Override
    public void initialize() {
        try {

            session = Pgx.createSession(SESSION_NAME);
        } catch (ExecutionException | InterruptedException e) {
            LOG.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void buildGraph(String configPath) throws FileNotFoundException {
        GraphUtils graphUtils = new GraphUtils();
        graph = graphUtils.loadGraph(session, configPath);
        distinctLabels = graphUtils.getDistinctEdgeLabelOccurrences(graph);

    }

    @Override
    public void buildGraph(Schema schema, String edgesFile, String verticesFile) throws IOException, InterruptedException, ExecutionException {
        GraphUtils graphUtils = new GraphUtils();
        graph = graphUtils.loadGraph(session, schema, edgesFile, verticesFile);
        distinctLabels = graphUtils.getDistinctEdgeLabelOccurrences(graph);

//        if (schema.isComputeConcatenationProperties())
//            sampliGraphByInputLabels(schema);
    }


    private void sampliGraphByInputLabels(Schema schema) throws ExecutionException, InterruptedException {

        Set<String> allowedLabels = new HashSet<>();
        for (String pairLabels : schema.getAllowedLabels()) {
            String[] pairs= pairLabels.split(":");
            for (int i = 0; i < pairs.length; i++)
                allowedLabels.add(String.format("edge.label() = '%s'", pairs[i]));

        }

        PgxGraph filteredGraph = graph.filter(new EdgeFilter(String.join(" || ", allowedLabels)));
        graph.destroy();
        graph = filteredGraph;
    }

   @Override
    public void updateGraph(Map<Integer,Set<Integer>> grouping) throws ExecutionException, InterruptedException {
        graph = GraphUtils.updateGraphGroupings(graph, grouping);
    }

    /**
     * Retrieve the list of labels sorted by frequency
     * @return the list of labels.
     */
    private Stream<Map.Entry<String, Integer>> getDistinctLabels() {
        return distinctLabels.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed());
    }

    @Override
    public List<String> getListOfLabels() {
        return getDistinctLabels().map(Map.Entry::getKey).collect(Collectors.toList());
    }

    @Override
    public void updateGraphGroupings(Map<Integer, SuperNode> grouping) throws ExecutionException, InterruptedException {
        GraphChangeSet<Integer> changeSet = graph.createChangeSet();

        /*
         * Since in the grouping map we have the label like an string
         *  @see GraphUtils#builtGraphFromConfig(PgxSession, FileGraphConfig)
         *  we have to use the substring for save the grouping.
         *
         */
        grouping.forEach((k,v) ->
            v.getVertices().forEach(val ->
                changeSet.updateVertex((Integer) val.getId()).setProperty(PrefixPropertyConstant.GROUPING.toString(),
                        k)
            )
        );
        graph = changeSet.build();
    }

    @Override
    public void cleanGroupings() throws ExecutionException, InterruptedException {
        graph = GraphUtils.cleanGroupingProperty(graph, false);
    }

    @Override
    public PgxGraph getGraph() {
        return graph;
    }


    @Override
    public PgxSession getSession() {
        return session;
    }


    @Override
    public void close() {
        session.close();
    }
}
