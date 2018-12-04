package label.driven.summarization.merge.classes;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/1/18.
 */
public class FrequencyEdge {
    private int idDstGN;
    private int idSrcGN;
    private int idSrcSN;
    private int idDstSN;
    private String label;
    private float edgeRatio;
    private float edgeWeight;
    private int nodeWeightSrc;
    private int nodeWeightDst;

    /* fixme */
    public FrequencyEdge(Integer idSrcGN, Integer idSrcSN, Integer idDstGN, Integer idDstSN, String label,
                         Float edgeRatio, Float edgeWeight, Float nodeWeightSrc, Float nodeWeightDst) {
        this.idSrcGN = idSrcGN;
        this.idDstGN = idDstGN;
        this.idSrcSN = idSrcSN;
        this.idDstSN = idDstSN;
        this.label = label;
        this.edgeRatio = edgeRatio;
        this.edgeWeight = edgeWeight;
        this.nodeWeightSrc = (int) (float) nodeWeightSrc;
        this.nodeWeightDst = (int) (float) nodeWeightDst;
    }

    public int getIdDstHN() {
        return idDstGN;
    }

    public String getLabel() {
        return label;
    }

    public float getEdgeRatio() {
        return edgeRatio;
    }

    public int getIdSrcGN() {
        return idSrcGN;
    }

    public int getNodeWeightDst() {
        return nodeWeightDst;
    }

    public int getNodeWeightSrc() {
        return nodeWeightSrc;
    }

    public int getIdSrcSN() {
        return idSrcSN;
    }

    public int getIdDstSN() {
        return idDstSN;
    }

    public float getEdgeWeight() {
        return edgeWeight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        FrequencyEdge that = (FrequencyEdge) o;

        return new EqualsBuilder()
                .append(idDstGN, that.idDstGN)
                .append(idSrcGN, that.idSrcGN)
                .append(idSrcSN, that.idSrcSN)
                .append(idDstSN, that.idDstSN)
                .append(label, that.label)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(idDstGN)
                .append(idSrcGN)
                .append(idSrcSN)
                .append(idDstSN)
                .append(label)
                .toHashCode();
    }
}
