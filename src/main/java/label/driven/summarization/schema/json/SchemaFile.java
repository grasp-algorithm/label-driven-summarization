package label.driven.summarization.schema.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;

import java.util.List;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/16/18.
 */
@JsonRootName("schema")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "allLabelsAllowed",
        "allowedLabels",
        "config",
        "vertexProperties"
})
public class SchemaFile {

    private SchemaFile() {
    }

    public SchemaFile(boolean computeConcatenationProperties, boolean allLabelsAllowed, List<String> allowedLabels, List<VertexProperty> vertexProperties, Config config) {
        this.computeConcatenationProperties = computeConcatenationProperties;
        this.allLabelsAllowed = allLabelsAllowed;
        this.allowedLabels = allowedLabels;
        this.vertexProperties = vertexProperties;
        this.config = config;
    }

    @JsonProperty("computeConcatenationProperties")
    private boolean computeConcatenationProperties;

    @JsonProperty("allLabelsAllowed")
    private boolean allLabelsAllowed;

    @JsonProperty("allowedLabels")
    private List<String> allowedLabels;

    @JsonProperty("config")
    private Config config = null;

    @JsonProperty("vertexProperties")
    private List<VertexProperty> vertexProperties = null;

    @JsonProperty("computeConcatenationProperties")
    public boolean isComputeConcatenationProperties() {
        return computeConcatenationProperties;
    }

    @JsonProperty("computeConcatenationProperties")
    public void setComputeConcatenationProperties(boolean computeConcatenationProperties) {
        this.computeConcatenationProperties = computeConcatenationProperties;
    }

    @JsonProperty("allLabelsAllowed")
    public boolean isAllLabelsAllowed() {
        return allLabelsAllowed;
    }

    @JsonProperty("allLabelsAllowed")
    public void setAllLabelsAllowed(boolean allLabelsAllowed) {
        this.allLabelsAllowed = allLabelsAllowed;
    }

    @JsonProperty("allowedLabels")
    public List<String> getAllowedLabels() {
        return allowedLabels;
    }

    @JsonProperty("allowedLabels")
    public void setAllowedLabels(List<String> allowedLabels) {
        this.allowedLabels = allowedLabels;
    }

    @JsonProperty("config")
    public Config getConfig() {
        return config;
    }

    @JsonProperty("config")
    public void setConfig(Config config) {
        this.config = config;
    }

    @JsonProperty("vertexProperties")
    public List<VertexProperty> getVertexProperties() {
        return vertexProperties;
    }

    @JsonProperty("vertexProperties")
    public void setVertexProperties(List<VertexProperty> vertexProperties) {
        this.vertexProperties = vertexProperties;
    }

}