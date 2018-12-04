package label.driven.summarization.merge.evaluation.histogram.mutation;

import label.driven.summarization.graph.Session;
import oracle.pgx.api.PgxEdge;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.VertexBuilder;
import oracle.pgx.api.VertexCollection;

import java.util.concurrent.ExecutionException;

/**
 * This class build a new Graph which will included all the vertices in the collection and
 * all the edges between them in the original graph.
 * @see Session#getGraph()
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 7/18/18.
 */
public interface Filter extends AutoCloseable {

    /**
     *
     * Returns a sub-graph
     *
     * @param session the current session
     * @param innerVertices a collection of vertices
     * @param grouping the current grouping to set the property
     * @return a subGraph with all the set of vertices
     * @throws ExecutionException if the sub-graph cannot be built.
     * @throws InterruptedException if the sub-graph cannot be built.
     */
    PgxGraph apply(Session session, VertexCollection<Integer> innerVertices, int grouping)
            throws ExecutionException, InterruptedException;


    /**
     *
     * Returns a sub-graph with the set of inner-vertices blocked.
     *
     * @param session the current session
     * @param graph the graph that we need to traverse
     * @param innerVertices a collection of vertices inside of the super-node
     * @param vertexCollection a collection of vertices inside of this new sub-graph
     * @param grouping the current grouping to set the property
     * @param block if you want add the property BLOCK, to identify the nodes inside of the super-node
     *              and outside
     * @return a subGraph with all the set of vertices
     * @throws ExecutionException if the sub-graph cannot be built.
     * @throws InterruptedException if the sub-graph cannot be built.
     */
    PgxGraph apply(Session session, PgxGraph graph, VertexCollection<Integer> innerVertices,
                   VertexCollection<Integer> vertexCollection, int grouping, boolean block)
            throws ExecutionException, InterruptedException;


    /**
     * Check if a vertex should be blocked
     *
     * @param innerVertices the set of vertices inside of the super-node
     * @param vb the vertex builder
     * @param edge the current edge.
     * @param isSrc if the vertex that we need treat is the source or the destination
     */
    void checkBlockingOfVertex(VertexCollection<Integer> innerVertices, VertexBuilder<Integer> vb, PgxEdge edge,
                               boolean isSrc);

}
