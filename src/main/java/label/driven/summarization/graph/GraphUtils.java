package label.driven.summarization.graph;

import label.driven.summarization.exception.ConversionAdjacencyMatrixException;
import label.driven.summarization.exception.LoadGraphDataException;
import label.driven.summarization.merge.Constants;
import label.driven.summarization.merge.PrefixPropertyConstant;
import label.driven.summarization.schema.Schema;
import label.driven.summarization.schema.json.Property;
import oracle.pgql.lang.PgqlException;
import oracle.pgx.api.*;
import oracle.pgx.api.filter.EdgeFilter;
import oracle.pgx.config.FileGraphConfig;
import oracle.pgx.config.IdGenerationStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * An auxiliary class for the data load and the build of the PGX Graph.
 * This class provides methods to load the input graph, update and remove the grouping property
 * on the vertices.
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/11/18.
 */
public class GraphUtils {

    private static final Logger LOG = LogManager.getLogger("GLOBAL");
    private static final String SEPARATOR_CONFIG = " ";
    private static volatile AtomicInteger idEdge = new AtomicInteger(-1);

    private static Integer getIdEdge() {
        return idEdge.incrementAndGet();
    }

    /**
     * This method built a graph from a configuration file sended by parameter.
     *
     * @param session    the PGX Session
     * @param configPath the location of the config file
     *
     * @return the graph loaded
     */
    PgxGraph loadGraph(PgxSession session, String configPath) throws FileNotFoundException {
        try {
            File configFile = new File(configPath);

            if (!configFile.exists())
                throw new FileNotFoundException(String.format("File %s not found.", configPath));

            return session.readGraphWithProperties(configPath);

        } catch (ExecutionException | InterruptedException e) {
            LOG.error(e.getMessage(), e);
            throw new LoadGraphDataException("Graph could not be loaded.");
        }
    }


    /**
     * This method built the PGX Graph taking into account the configuration object where is described
     * the data file.
     * <p>
     * Attention: This method is prepare to process a file with the output format from gMark.
     * Check the gMark project here: https://github.com/graphMark/gmark
     * Check the file format here: https://github.com/graphMark/gmark/blob/master/demo/test/test-graph.txt
     * <p>
     * Due to the requirement of Pgx, we need to transform the label (which is numerical) to string
     * So we concatenate the letter `l` (from label) before the number.
     *
     * @param session the PGX Session
     * @param config  the configuration object for the data load.
     *
     * @return a graph built using the configuration object for retrieve the file.
     */
    PgxGraph builtGraphFromConfig(PgxSession session, FileGraphConfig config) {
        try (Scanner scanner = new Scanner(new File(config.getUri()))) {
            GraphBuilder<Integer> builder = session.createGraphBuilder();
            int index = 0;

            while (scanner.hasNext()) {
                String[] line = scanner.nextLine().split(SEPARATOR_CONFIG);
                String labelPrefix = "";
                if (line[2].chars().allMatch(Character::isDigit))
                    labelPrefix = GraphSession.LABEL_PREFIX;

                builder.addEdge(index, Integer.parseInt(line[0]), Integer.parseInt(line[1]))
                        .setLabel(labelPrefix.concat(line[2]));
                index++;
            }

            return builder.build();

        } catch (ExecutionException | InterruptedException e) {
            LOG.error(e.getMessage(), e);
            throw new LoadGraphDataException("Graph could not be built.");
        } catch (FileNotFoundException e) {
            LOG.error(e.getMessage(), e);
            throw new ConversionAdjacencyMatrixException("Conversion could not be completed");
        }
    }

    public static PgxGraph getFilteredGraph(Schema schema, PgxGraph graph) throws ExecutionException, InterruptedException {
        PgxGraph filteredGraph = graph;
        if (!schema.isAllLabelsAllowed()) {

            Set<String> filteredLabels = new HashSet<>();
            schema.getAllowedLabels().forEach(pair -> {
                List<String> labels = Arrays.asList(pair.split(":"));
                filteredLabels.add(String.format("edge.label() = '%s'", labels.get(0)));
                filteredLabels.add(String.format("edge.label() = '%s'", labels.get(1)));
            });

            filteredGraph = graph.filter(new EdgeFilter(filteredLabels.stream().collect(Collectors.joining(" || "))));
        }

        return filteredGraph;
    }


    public static PgxGraph getFilteredGraph(List<String> labels, PgxGraph graph) throws ExecutionException, InterruptedException {
        Set<String> filteredLabels = new HashSet<>();

        for (String label : labels)
            filteredLabels.add(String.format("edge.label() = '%s'", label));

        return graph.filter(new EdgeFilter(filteredLabels.stream().collect(Collectors.joining(" || "))));
    }


    /**
     * @param session    the current session.
     * @param schema     the schema of the input graph, is mandatory.
     * @param edgeFile   the file with the edges of the input graph, is mandatory
     * @param vertexFile not mandatory, the graphs without attributes doesn't need them
     *
     * @return the loaded graph.
     * @throws IOException if some file doesn't exists or the system doesn't have privileges to read/write in it.
     */
    PgxGraph loadGraph(PgxSession session, Schema schema, String edgeFile, String vertexFile) throws IOException,
            ExecutionException, InterruptedException {

        Objects.requireNonNull(schema);
        Objects.requireNonNull(edgeFile);
        if (!new File(edgeFile).exists())
            throw new FileNotFoundException(String.format("File '%s' not found.", edgeFile));

        GraphBuilder<Integer> builder = session.createGraphBuilder(IdGenerationStrategy.USER_IDS, IdGenerationStrategy.USER_IDS);

        if (vertexFile != null && new File(vertexFile).exists())
            loadVertexFile(builder, schema, vertexFile);

        try (FileInputStream inputStream = new FileInputStream(edgeFile);
             Scanner sc = new Scanner(inputStream, "UTF-8")) {

            while (sc.hasNextLine()) {

                String[] line = sc.nextLine().split(SEPARATOR_CONFIG);

                String labelPrefix = "";
                if (line[1].chars().allMatch(Character::isDigit))
                    labelPrefix = GraphSession.LABEL_PREFIX;

                builder.addEdge(getIdEdge(),
                        Integer.parseInt(line[0]), Integer.parseInt(line[2]))
                        .setLabel(labelPrefix.concat(line[1]));
            }

            // note that Scanner suppresses exceptions
            if (sc.ioException() != null) {
                throw sc.ioException();
            }

        }

        return builder.build();
    }


    /**
     * This method will read the vertex-file and will add the vertices to the builder graph.
     *
     * @param builder    the builder graph
     * @param schema     the schema of the input graph
     * @param vertexFile the vertex file
     *
     * @throws IOException if it's not possible to read the vertex file.
     */
    private void loadVertexFile(GraphBuilder<Integer> builder, Schema schema, String vertexFile) throws IOException {

        try (FileInputStream inputStream = new FileInputStream(vertexFile);
             Scanner sc = new Scanner(inputStream, "UTF-8")) {

            while (sc.hasNextLine()) {
                String[] line = sc.nextLine().split(SEPARATOR_CONFIG);

                int counter = 0;
                VertexBuilder<Integer> vertexBuilder = builder.addVertex(Integer.parseInt(line[counter++]));

                if (line.length > 1 && line[1].contains("\""))
                    vertexBuilder.addLabel(line[counter++]);
                else {
                    schema.getAllLabels().forEach(vertexBuilder::addLabel);
                }

                if (line.length > counter && !line[counter].contains("\"")) {
                    Property prop = schema.getAllVertexProperties().get(counter);
                    vertexBuilder.setProperty(prop.getName(), line[counter]);
                }
            }

            // note that Scanner suppresses exceptions
            if (sc.ioException() != null) {
                throw sc.ioException();
            }

        }
    }

    /**
     * This method retrieve the label on the edges and the number of the occurrences for each one.
     *
     * @param graph the original graph
     *
     * @return list of distinct labels on edges
     */
    Map<String, Integer> getDistinctEdgeLabelOccurrences(PgxGraph graph) {

        Map<String, Integer> labelList = new HashMap<>();

        try {
            PgqlResultSet results = graph.queryPgql("SELECT label(e), COUNT(*) MATCH () -[e]-> () GROUP BY label(e)");
            for (PgxResult result : results)
                labelList.put(result.getString(1), result.getLong(2).intValue());

        } catch (PgqlException | ExecutionException | InterruptedException e) {
            LOG.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

        return labelList;
    }

    private static PgxGraph cleanGroupingProperty(PgxGraph graph) throws ExecutionException, InterruptedException {

        GraphChangeSet<Integer> changeSet = graph.createChangeSet();
        graph.getVertices().forEach(ov -> changeSet.updateVertex((Integer) ov.getId())
                .setProperty(PrefixPropertyConstant.GROUPING.toString(), Constants.GROUPING_DEFAULT_VALUE));
        return changeSet.build();
    }

    public static PgxGraph cleanGroupingProperty(PgxGraph graph, boolean floatProperty) throws ExecutionException, InterruptedException {
        if (floatProperty)
            return cleanGroupingProperty(graph);
        else {

            GraphChangeSet<Integer> changeSet = graph.createChangeSet();
            graph.getVertices().forEach(ov -> changeSet.updateVertex((Integer) ov.getId())
                    .setProperty(PrefixPropertyConstant.GROUPING.toString(), (int) Constants.GROUPING_DEFAULT_VALUE));
            return changeSet.build();
        }
    }

    public static PgxGraph updateGraph(PgxGraph graph, Map<String, Set<Integer>> grouping) throws ExecutionException, InterruptedException {

        GraphChangeSet<Integer> changeSet = graph.createChangeSet();

        /*
         * Since in the grouping map we have the label like an string
         *  @see GraphUtils#builtGraphFromConfig(PgxSession, FileGraphConfig)
         *  we have to use the substring for save the grouping.
         *
         */
        grouping.forEach((k, v) ->
                        v.forEach(val -> changeSet.updateVertex(val).setProperty(
                                PrefixPropertyConstant.GROUPING.toString(), Float.parseFloat(k)))
                         );
        return changeSet.build();

    }


    static PgxGraph updateGraphGroupings(PgxGraph graph, Map<Integer, Set<Integer>> grouping) throws ExecutionException,
            InterruptedException {

        GraphChangeSet<Integer> changeSet = graph.createChangeSet();

        /*
         * Since in the grouping map we have the label like an string
         *  @see GraphUtils#builtGraphFromConfig(PgxSession, FileGraphConfig)
         *  we have to use the substring for save the grouping.
         *
         */
        grouping.forEach((k, v) ->
                        v.forEach(val -> changeSet.updateVertex(val).setProperty(
                                PrefixPropertyConstant.GROUPING.toString(), k))
                         );
        return changeSet.build();

    }
}
