package label.driven.summarization.schema.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
/**
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/16/18.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "label",
        "allowProperties"
})
public class VertexProperty {

    @JsonProperty("label")
    private String label;
    @JsonProperty("allowProperties")
    private List<AllowProperty> allowProperties = null;

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    @JsonProperty("label")
    public void setLabel(String label) {
        this.label = label;
    }

    @JsonProperty("allowProperties")
    public List<AllowProperty> getAllowProperties() {
        return allowProperties;
    }

    @JsonProperty("allowProperties")
    public void setAllowProperties(List<AllowProperty> allowProperties) {
        this.allowProperties = allowProperties;
    }
}