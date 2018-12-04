package label.driven.summarization.merge.classes;

import oracle.pgx.api.PgxVertex;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/25/18.
 */
public class ReachabilityLabel {

    private PgxVertex vertex;
    private String frequentLabel;
    private Float reachabilityPaths;

    public ReachabilityLabel(PgxVertex vertex, String frequentLabel, Float reachabilityPaths) {
        this.vertex = vertex;
        this.frequentLabel = frequentLabel;
        this.reachabilityPaths = reachabilityPaths;
    }

    public PgxVertex getVertex() {
        return vertex;
    }

    public String getFrequentLabel() {
        return frequentLabel;
    }

    public Float getReachabilityPaths() {
        return reachabilityPaths;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        ReachabilityLabel that = (ReachabilityLabel) o;

        return new EqualsBuilder()
                .append(vertex, that.vertex)
                .append(frequentLabel, that.frequentLabel)
                .append(reachabilityPaths, that.reachabilityPaths)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(vertex)
                .append(frequentLabel)
                .append(reachabilityPaths)
                .toHashCode();
    }
}
