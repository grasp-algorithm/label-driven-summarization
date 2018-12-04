package label.driven.summarization.merge.classes;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/1/18.
 */
public class CrossEdge {

    private Long idxEdge;
    private Integer idSrcGN;
    private Integer idDstGN;
    private String label;
    private Float ratio;
    private Float weight;

    public CrossEdge(Integer idSrcGN, Integer idDstGN, String label) {
        this.idSrcGN = idSrcGN;
        this.idDstGN = idDstGN;
        this.label = label;
    }

    public CrossEdge(Integer idSrcGN, Integer idDstGN, String label, Float ratio) {
        this.idSrcGN = idSrcGN;
        this.idDstGN = idDstGN;
        this.label = label;
        this.ratio = ratio;
    }

    public Integer getIdSrcHN() {
        return idSrcGN;
    }

    public Integer getIdDstHN() {
        return idDstGN;
    }

    public String getLabel() {
        return label;
    }

    public Float getRatio() {
        return ratio;
    }

    public Long getIdxEdge() {
        return idxEdge;
    }

    public Float getWeight() {
        return weight;
    }

    public void setRatio(Float ratio) {
        this.ratio = ratio;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }

    public void setIdxEdge(Long idxEdge) {
        this.idxEdge = idxEdge;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        CrossEdge crossEdge = (CrossEdge) o;

        return new EqualsBuilder()
                .append(idSrcGN, crossEdge.idSrcGN)
                .append(idDstGN, crossEdge.idDstGN)
                .append(label, crossEdge.label)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(idSrcGN)
                .append(idDstGN)
                .append(label)
                .toHashCode();
    }
}