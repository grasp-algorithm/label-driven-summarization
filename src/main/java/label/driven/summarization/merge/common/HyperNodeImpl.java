package label.driven.summarization.merge.common;

import oracle.pgx.api.PgxVertex;

import java.util.Set;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/24/18.
 */
public class HyperNodeImpl implements HyperNode {
    private float avgWeight;
    private int grouping;
    private int id;
    private Set<PgxVertex> vertices;

    public HyperNodeImpl(int grouping, int index, Set<PgxVertex> vertices) {
        this.grouping = grouping;
        this.id = index;
        this.vertices = vertices;
    }

    public HyperNodeImpl(int grouping, int id, Set<PgxVertex> vertices, float avgWeight) {
        this.grouping = grouping;
        this.id = id;
        this.vertices = vertices;
        this.avgWeight = avgWeight;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getGrouping() {
        return grouping;
    }

    public float getAvgWeight() {
        return avgWeight;
    }

    @Override
    public Set<PgxVertex> getVertices() {
        return vertices;
    }

}