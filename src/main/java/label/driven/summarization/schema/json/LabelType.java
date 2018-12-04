package label.driven.summarization.schema.json;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/16/18.
 */
public enum LabelType {
    ALL("ALL");

    private String description;

    LabelType(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
