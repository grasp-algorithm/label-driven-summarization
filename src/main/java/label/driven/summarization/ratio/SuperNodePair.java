package label.driven.summarization.ratio;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/12/18.
 */
public class SuperNodePair {
    private String superNodeI;
    private String superNodeJ;
    private String label;

    SuperNodePair(Integer superNodeI, Integer superNodeJ, String label) {
        this.superNodeI = String.valueOf(superNodeI);
        this.superNodeJ = String.valueOf(superNodeJ);
        this.label = label;
    }

    String getSuperNodeI() {
        return superNodeI;
    }

    String getSuperNodeJ() {
        return superNodeJ;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        SuperNodePair that = (SuperNodePair) o;

        return new EqualsBuilder()
                .append(superNodeI, that.superNodeI)
                .append(superNodeJ, that.superNodeJ)
                .append(label, that.label)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(superNodeI)
                .append(superNodeJ)
                .append(label)
                .toHashCode();
    }
}
