package label.driven.summarization.schema.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/1/18.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public enum VertexMergeStrategy {
    EVALUATION,
    FREQUENCY_SOURCE_NODES,
    FREQUENCY_SINK_NODES,
    INNER_MERGE_EQUALITY,
    INNER_MERGE_FREQ_LABEL,
    TARGET_MERGE,
    SOURCE_MERGE,
    WEIGHT_MERGE,
    CLUSTERING_EDGES;

    private static Map<String, VertexMergeStrategy> namesMap = new HashMap<>(2);

    static {
        namesMap.put("EVALUATION", EVALUATION);
        namesMap.put("FREQUENCY_SOURCE_NODES", FREQUENCY_SOURCE_NODES);
        namesMap.put("FREQUENCY_SINK_NODES", FREQUENCY_SINK_NODES);
        namesMap.put("INNER_MERGE_EQUALITY", INNER_MERGE_EQUALITY);
        namesMap.put("INNER_MERGE_FREQ_LABEL", INNER_MERGE_FREQ_LABEL);
        namesMap.put("TARGET_MERGE", TARGET_MERGE);
        namesMap.put("SOURCE_MERGE", SOURCE_MERGE);
        namesMap.put("WEIGHT_MERGE", WEIGHT_MERGE);
        namesMap.put("CLUSTERING_EDGES", CLUSTERING_EDGES);
    }

    @JsonCreator
    public static VertexMergeStrategy forValue(String value) {
        return namesMap.get(value.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        for (Map.Entry<String, VertexMergeStrategy> entry : namesMap.entrySet()) {
            if (entry.getValue() == this)
                return entry.getKey();
        }

        return null;
    }
}
