package label.driven.summarization.merge.classes;

import oracle.pgx.api.PgxVertex;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.List;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/1/18.
 */
public class PgxVertexByLabel {

    private PgxVertex vertex;
    private List<String> outGoingLabels;
    private List<String> inGoingLabels;
    private Float frequency;
    private String label;

    public PgxVertexByLabel(PgxVertex vertex, List<String> outGoingLabels,
                            List<String> inGoingLabels, Float frequency) {
        this.vertex = vertex;
        this.outGoingLabels = outGoingLabels;
        this.inGoingLabels = inGoingLabels;
        this.frequency = frequency;
    }

    public PgxVertexByLabel(PgxVertex vertex, List<String> outGoingLabels,
                            List<String> inGoingLabels, Float frequency, String label) {
        this.vertex = vertex;
        this.outGoingLabels = outGoingLabels;
        this.inGoingLabels = inGoingLabels;
        this.frequency = frequency;
        this.label = label;
    }

    public PgxVertex getVertex() {
        return vertex;
    }

    public List<String> getOutGoingLabels() {
        return outGoingLabels;
    }

    public List<String> getInGoingLabels() {
        return inGoingLabels;
    }

    public Float getFrequency() {
        return frequency;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        PgxVertexByLabel that = (PgxVertexByLabel) o;

        return new EqualsBuilder()
                .append(vertex, that.vertex)
                .append(outGoingLabels, that.outGoingLabels)
                .append(inGoingLabels, that.inGoingLabels)
                .append(frequency, that.frequency)
                .append(label, that.label)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(vertex)
                .append(outGoingLabels)
                .append(inGoingLabels)
                .append(frequency)
                .append(label)
                .toHashCode();
    }
}