package label.driven.summarization.merge.evaluation.histogram;

import oracle.pgx.api.VertexCollection;

import java.util.Map;

/**
 * This class compute the properties of each super-node.
 * Either the vertex or edge properties.
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/22/18.
 */
public interface Histogram {

    /**
     * This method implements the strategy to calculate the new value on the histogram of
     * a property (e.g. the percentage, ...)
     *
     * @param totalValue the sum of the appearances of a fact in the set
     * @param occurrences the total number of facts on the set
     * @return the new value which will be stored on the histogram.
     */
    Float computeValue(float totalValue, float occurrences);

    /**
     * Compute the property grouping.
     * @see label.driven.summarization.merge.PrefixPropertyConstant#GROUPING
     *
     * @param vertices the list of vertices which will be included in the super-node
     */
    void computeGrouping(VertexCollection<Integer> vertices);

    /**
     * Compute the vertex properties like inner-nodes
     * @see label.driven.summarization.merge.PrefixPropertyConstant#NODE_WEIGHT_EVALUATION
     *
     * @param vertices the list of vertices which will be included in the super-node
     */
    void computeVertexPropertiesOnHistogram(VertexCollection<Integer> vertices);


    /**
     * Compute the edge properties like inner-edges, percentage of labels, reachability count, etc.
     * @see label.driven.summarization.merge.PrefixPropertyConstant#NUMBER_OF_INNER_EDGES
     *
     * @param pgxVertices the list of vertices which will be included in the super-node
     */
    void computeEdgePropertiesOnHistogram(VertexCollection<Integer> pgxVertices);

    /**
     * Return the list of properties
     * @return the list of properties of the current super-node
     */
    Map<String, Float> getVertexEdgeProperties();

}
