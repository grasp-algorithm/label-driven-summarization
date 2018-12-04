package label.driven.summarization.graph;

import label.driven.summarization.merge.common.SuperNode;
import label.driven.summarization.schema.Schema;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.PgxSession;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * This class preserve the PGX Session.
 * Here the methods to manipulate the input graph.
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 7/13/18.
 */
public interface Session {

    /**
     * Initialize the PGX session.
     */
    void initialize();


    /**
     * This method allow the data load on the graph
     *
     * @param configPath path of the config file
     *
     * @throws FileNotFoundException if the config file does not exists.
     * @see GraphUtils#loadGraph(PgxSession, String)
     */
    void buildGraph(String configPath) throws FileNotFoundException;

    /**
     * This method load the input graph in the PGX Structure. The input file for the edge must be
     * in the gMark format.
     * Check the gMark project here: https://github.com/graphMark/gmark
     *
     * @param schema the mandatory schema of the input graph
     * @param edgesFile the file with the description of all the edges with the gMark format
     * @param verticesFile the file with the list of vertices and properties (not currently supported)
     *
     * @throws IOException if the files are not found
     * @throws InterruptedException if it's not possible load the graph.
     * @throws ExecutionException if it's not possible load the graph.
     */
    void buildGraph(Schema schema, String edgesFile, String verticesFile) throws IOException, InterruptedException, ExecutionException;


    /**
     * This method update the graph modify the grouping property and updated to the value in the input map.
     *
     * @param grouping the set of grouping and the list of IDs of vertices
     * @throws ExecutionException if it's nor possible to modify the value of the property.
     * @throws InterruptedException if it's nor possible update some vertex
     */
    void updateGraph(Map<Integer, Set<Integer>> grouping) throws ExecutionException, InterruptedException;

    /**
     * This method update the graph adding the property `grouping` after apply some grouping strategy.
     *
     * @param grouping the grouping of our choice
     *
     * @throws ExecutionException   if it's not possible retrieve the vertex on the original graph.
     * @throws InterruptedException if it's not possible update the selected vertex.
     */
    void updateGraphGroupings(Map<Integer, SuperNode> grouping) throws ExecutionException, InterruptedException;

    /**
     * This method update all the grouping properties to `-1`, to allow the identification of the vertices without
     * any grouping after a grouping.
     *
     * @throws ExecutionException if it's not possible retrieve the vertex on the original graph.
     * @throws InterruptedException if it's not possible update the selected vertex.
     */
    void cleanGroupings() throws ExecutionException, InterruptedException;

    /**
     * The list of labels sorted by number of occurrences.
     * @return the list of labels.
     */
    List<String> getListOfLabels();

    /**
     * Get the input graph.
     *
     * @return returns the input graph.
     */
    PgxGraph getGraph();

    /**
     * Get the PGX session
     *
     * @return the current PGX session
     */
    PgxSession getSession();

    /**
     * Close the PGX Session.
     */
    void close();
}
