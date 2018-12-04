package label.driven.summarization.schema;

import label.driven.summarization.schema.json.Config;
import label.driven.summarization.schema.json.Property;

import java.util.List;
import java.util.Set;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/18/18.
 */
public interface Schema {

    /**
     *
     * @return
     */
    boolean isComputeConcatenationProperties();

    /**
     *
     * @return
     */
    boolean isAllLabelsAllowed();

    /**
     *
     * @return
     */
    List<String> getAllowedLabels();

    /**
     * The configuration has the vertex merge strategy and the epsilon values (relaxation factor)
     * @return the current configuration
     */
    Config getConfig();

    /**
     * This method return all the vertex properties
     * @return all vertex properties
     */
    List<Property> getAllVertexProperties();

    /**
     * Return the list of la.getAllVertexProperties()bels on the schema
     *
     * @return list of labels.
     */
    List<String> getAllLabels();

    /**
     * This method return a set of valid properties that corresponds to the given label
     *
     * @param label the selected label
     * @return the list of all properties by the given set of labels
     */
    List<Property> getAllVertexPropertiesByLabel(String label);

    /**
     * Return the set of possible values given a label.
     *
     * @param label the vertex label
     * @return the set of all possible values of all the properties of the given label.
     */
    Set<String> getAllPossibleValuesByLabel(String label);

    /**
     * Return all the labels by the given property
     *
     * @param property the property of the vertex
     * @return the set of labels which use this property.
     */
    Set<String> getLabelsByProperty(String property);

    /**
     *
     * @return the list of labels in the schema file
     */
    Set<String> getVerticesLabels();
}
