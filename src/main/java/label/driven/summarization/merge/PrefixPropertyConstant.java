package label.driven.summarization.merge;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/24/18.
 */
public enum PrefixPropertyConstant {

    NODE_WEIGHT_EVALUATION("NUMBER_OF_NODES_INSIDE_OF_SN"),
    NODE_WEIGHT_FREQUENCY("NUMBER_OF_SN_INSIDE_OF_HN"),
    NODE_WEIGHT_EVALUATION_AVG("AVG_WEIGHT_ON_SN"),
    PERCENTAGE("PERCENTAGE"),
    NUMBER_OF_INNER_EDGES("NUMBER_OF_INNER_EDGES"),
    LABEL("LABEL"),
    EDGE_RATIO("EDGE_RATIO"),
    GROUPING("GROUPING"),
    REACHABILITY_COUNT("REACH_NUMBER_OF_INNER_PATHS"),
    PATH_OUT("REACH_PATH_OUT_BY_LABEL"),
    PATH_IN("REACH_PATH_IN_BY_LABEL"),
    EDGE_WEIGHT("EDGE_WEIGHT"),
    PARTICIPATION_LABEL("PARTICIPATION_LABEL"),
    TRAVERSAL_FRONTIERS("TRAVERSAL_FRONTIERS");

    private String description;

    PrefixPropertyConstant(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
