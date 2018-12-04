package label.driven.summarization.util;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/18/18.
 */
public interface EdgeRatioRow {

    /**
     *
     * @param numerator
     * @param divider
     * @return
     */
    float computeRatio(float numerator, float divider);

    /**
     *
     * @return
     */
    String getLabel();

    /**
     *
     * @return
     */
    float getRatio();

    /**
     *
     * @return
     */
    Relationship getTypeRelationship();

    /**
     *
     * @return
     */
    Integer getGroupI();

    /**
     *
     * @return
     */
    Integer getGroupJ();


    /**
     *
     * @return
     */
    Float getEdgeWeight();

    /**
     *
     * @return
     */
    String getKey();
}
