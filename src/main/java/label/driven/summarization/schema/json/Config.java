package label.driven.summarization.schema.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Set;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/1/18.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "epsilon",
        "mergingProcedure"
})
public class Config {
    @JsonProperty("epsilon")
    private int epsilon;

    @JsonProperty("theta")
    private float theta;

    @JsonProperty("mergingProcedure")
    private Set<VertexMergeStrategy> mergingProcedure = null;

    @JsonProperty("epsilon")
    public int getEpsilon() {
        return epsilon;
    }

    @JsonProperty("epsilon")
    public void setEpsilon(int epsilon) {
        this.epsilon = epsilon;
    }

    @JsonProperty("theta")
    public float getTheta() {
        return theta;
    }

    @JsonProperty("theta")
    public void setTheta(float theta) {
        this.theta = theta;
    }

    @JsonProperty("mergingProcedure")
    public Set<VertexMergeStrategy> getMergingProcedure() {
        return mergingProcedure;
    }

    @JsonProperty("mergingProcedure")
    public void setMergingProcedure(Set<VertexMergeStrategy> mergingProcedure) {
        this.mergingProcedure = mergingProcedure;
    }
}
