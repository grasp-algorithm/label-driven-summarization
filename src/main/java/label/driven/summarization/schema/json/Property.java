package label.driven.summarization.schema.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * The property that can have an specific label on the schema.
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/19/18.
 */
public interface Property {

    /**
     * the name of the property
     * @return the name of the property
     */
    String getName();

    /**
     * the type of the property (e.g. integer, string, etc.)
     * @return the type of the property
     */
    String getType();

    /**
     * the values specified on the config file
     * @return the values specified in the config file
     */
    String getValues();

    /**
     * This method return the merge strategy between property during the step of evaluation
     * @return the property merge strategy
     */
    PropertyMergeStrategy getMergeStrategy();

    /**
     * The list of possible values if the specified value is expressed like a regular expression with disjunctions.
     *
     * @return the list of possible values
     */
    default List<String> getPossibleValues() {
        if (getMergeStrategy().equals(PropertyMergeStrategy.IGNORE))
            return new ArrayList<>();

        if (!getMergeStrategy().equals(PropertyMergeStrategy.VARIANCE))
            return Arrays.asList(getValues().split("\\|"));
        else {
            List<String> list = new ArrayList<>();
            list.add(PropertyMergeStrategy.VARIANCE.toValue());

            return list;
        }

    }

}
