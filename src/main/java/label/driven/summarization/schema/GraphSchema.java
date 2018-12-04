package label.driven.summarization.schema;

import label.driven.summarization.exception.NotLabelFoundException;
import label.driven.summarization.schema.json.LabelType;
import label.driven.summarization.schema.json.SchemaFile;
import label.driven.summarization.schema.json.Property;

import java.util.*;

/**
 * This class manage the graph schema, given an input file with the configuration of the vertex properties
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/16/18.
 */
public class GraphSchema extends SchemaFile implements Schema {

    GraphSchema(SchemaFile schema) {
        super(schema.isComputeConcatenationProperties(), schema.isAllLabelsAllowed(), schema.getAllowedLabels(), schema.getVertexProperties(), schema.getConfig());
    }

    @Override
    public List<Property> getAllVertexProperties() {
        List<Property> allProperties = new ArrayList<>();

        this.getVertexProperties().forEach(vp -> allProperties.addAll(vp.getAllowProperties()));

        return allProperties;
    }

    @Override
    public List<String> getAllLabels() {
        List<String> labels = new ArrayList<>();
        getVertexProperties().forEach(vp -> labels.add(vp.getLabel()));

        labels.removeIf(v -> v.equals("ALL"));
        return labels;
    }

    @Override
    public List<Property> getAllVertexPropertiesByLabel(String label) {
        List<Property> allProperties = new ArrayList<>();

        this.getVertexProperties().forEach(vp -> {
            if (label.equals(vp.getLabel()) || vp.getLabel().equals(LabelType.ALL.toString()))
                allProperties.addAll(vp.getAllowProperties());
        });

        return allProperties;
    }

    @Override
    public Set<String> getAllPossibleValuesByLabel(String label) {
        Set<String> possibleValues = new HashSet<>();

        getVertexProperties().stream().filter(vp -> vp.getLabel().equals(label)).findAny()
                .ifPresent(vp -> vp.getAllowProperties().forEach(ap ->
                        possibleValues.addAll(ap.getPossibleValues())));

        return possibleValues;
    }


    @Override
    public Set<String> getLabelsByProperty(String property) {
        Set<String> labels = new HashSet<>();

        try {
            getVertexProperties().forEach(vp -> {
                if (vp.getAllowProperties().stream().anyMatch(ap -> ap.getName().equals(property)))
                    labels.add(vp.getLabel());
            });
        } catch (IllegalStateException e) {
            throw new NotLabelFoundException("Not label found on vertices.");
        }

        return labels;
    }


    public Set<String> getVerticesLabels() {
        Set<String> labels = new HashSet<>();
        getVertexProperties().forEach(vp -> labels.add(vp.getLabel()));

        return labels;

    }

}
