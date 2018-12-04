package label.driven.summarization.schema.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum class for serialize/deserialize with jackson
 * https://stackoverflow.com/a/20421494/5535262
 *
 * Three strategies to the merge of properties
 * * COUNT_OCCURRENCES: Basically the percentage of occurrences
 * * VARIANCE: We set buckets
 * * IGNORE: We ignore the property, we're not interested on merging it.
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/19/18.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public enum PropertyMergeStrategy {
    COUNT_OCCURRENCES,
    VARIANCE,
    IGNORE;

    private static Map<String, PropertyMergeStrategy> namesMap = new HashMap<>(2);

    static {
        namesMap.put("COUNT_OCCURRENCES", COUNT_OCCURRENCES);
        namesMap.put("VARIANCE", VARIANCE);
        namesMap.put("IGNORE", IGNORE);
    }

    @JsonCreator
    public static PropertyMergeStrategy forValue(String value) {
        return namesMap.get(value.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        for (Map.Entry<String, PropertyMergeStrategy> entry : namesMap.entrySet()) {
            if (entry.getValue() == this)
                return entry.getKey();
        }

        return null;
    }
}
