package label.driven.summarization.merge.common;

import oracle.pgx.api.VertexCollection;

/**
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/23/18.
 */
public class SuperNodeImpl implements SuperNode {

    private int grouping;
    private int id;
    private VertexCollection<Integer> vertices;

    public SuperNodeImpl(int grouping, int id, VertexCollection<Integer> vertices) {
        this.grouping = grouping;
        this.id = id;
        this.vertices = vertices;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getGrouping() {
        return grouping;
    }

    @Override
    public VertexCollection<Integer> getVertices() {
        return vertices;
    }

}