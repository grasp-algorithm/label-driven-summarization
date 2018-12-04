package label.driven.summarization.util;

/**
 *
 * Implementation of the participation ratio taking into account the presence of labeled edges.
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/15/18.
 */
public class EdgeRatioRowImpl implements EdgeRatioRow {
    private String label;
    private Integer groupI;
    private Integer groupJ;
    private float ratio;
    private Float edgeWeight;

    public EdgeRatioRowImpl(String label, Integer groupI, Integer groupJ, Float edgeWeight) {
        this.label = label;
        this.groupI = groupI;
        this.groupJ = groupJ;
        this.edgeWeight = edgeWeight;
    }

    public EdgeRatioRowImpl(String label, Integer groupI, Integer groupJ, Float edgeWeight, float ratio) {
        this.label = label;
        this.groupI = groupI;
        this.groupJ = groupJ;
        this.ratio = ratio;
        this.edgeWeight = edgeWeight;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public float getRatio() {
        return ratio;
    }

    @Override
    public float computeRatio(float numerator, float divider) {
        ratio = numerator / divider;
        return getRatio();
    }

    @Override
    public Relationship getTypeRelationship() {
        if (ratio > 0.5f)
            return Relationship.STRONG;
        else
            return Relationship.WEAK;
    }

    @Override
    public Integer getGroupI() {
        return groupI;
    }

    @Override
    public Integer getGroupJ() {
        return groupJ;
    }

    @Override
    public Float getEdgeWeight() {
        return edgeWeight;
    }

    @Override
    public String getKey() {
        return "Label: ".concat(label).concat(" Group_{").concat(String.valueOf(groupI))
                .concat("} and Group_{").concat(String.valueOf(groupJ)).concat("} ");
    }
}
