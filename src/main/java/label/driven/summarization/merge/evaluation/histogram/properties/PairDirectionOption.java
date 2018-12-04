package label.driven.summarization.merge.evaluation.histogram.properties;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 7/9/18.
 */
public enum PairDirectionOption {

    IN_IN("_IN_IN"),
    OUT_OUT("_OUT_OUT"),
    IN_OUT("_IN_OUT"),
    OUT_IN("_OUT_IN");

    private String description;

    PairDirectionOption(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }


}
