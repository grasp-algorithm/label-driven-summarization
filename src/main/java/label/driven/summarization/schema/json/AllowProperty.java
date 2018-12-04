package label.driven.summarization.schema.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/16/18.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "name",
        "type",
        "values",
        "mergeStrategy"
})
public class AllowProperty implements Property {

    @JsonProperty("name")
    private String name;
    @JsonProperty("type")
    private String type;
    @JsonProperty("values")
    private String values;
    @JsonProperty("mergeStrategy")
    private PropertyMergeStrategy mergeStrategy;

    @Override
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @Override
    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    @Override
    @JsonProperty("values")
    public String getValues() {
        return values;
    }

    @JsonProperty("values")
    public void setValues(String values) {
        this.values = values;
    }

    @JsonProperty("mergeStrategy")
    public PropertyMergeStrategy getMergeStrategy() {
        return mergeStrategy;
    }

    @JsonProperty("mergeStrategy")
    public void setMergeStrategy(PropertyMergeStrategy mergeStrategy) {
        this.mergeStrategy = mergeStrategy;
    }
}