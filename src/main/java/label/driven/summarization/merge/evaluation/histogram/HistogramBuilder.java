package label.driven.summarization.merge.evaluation.histogram;

import label.driven.summarization.graph.Session;
import label.driven.summarization.schema.Schema;
import oracle.pgx.api.VertexCollection;

/**
 *
 * This class allows the creation of histogram sending the required parameters.
 * @see this#build()
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/16/18.
 */
public class HistogramBuilder {
    private Schema schema;
    private VertexCollection<Integer> vertices;
    private Session session;
    private int grouping;

    /**
     *
     * @param schema the schema of the dataset
     */
    public HistogramBuilder(Schema schema) {
        this.schema = schema;
    }

    public Schema getSchema() {
        return schema;
    }

    public HistogramBuilder setSchema(Schema schema) {
        this.schema = schema;
        return this;
    }

    public HistogramBuilder setVertices(VertexCollection<Integer> vertices) {
        this.vertices = vertices;
        return this;
    }

    public HistogramBuilder setSession(Session session) {
        this.session = session;
        return this;
    }

    public HistogramBuilder setGrouping(int grouping) {
        this.grouping = grouping;
        return this;
    }

    public Session getSession() {
        return session;
    }

    public int getGrouping() {
        return grouping;
    }

    /**
     * This method build a histogram
     * @return a histogram with all the properties of the new super-node.
     */
    public Histogram build() {
        Histogram histogram = new HistogramImpl(this);
        histogram.computeGrouping(this.vertices);
        histogram.computeVertexPropertiesOnHistogram(this.vertices);
        histogram.computeEdgePropertiesOnHistogram(this.vertices);

        return histogram;
    }
}
