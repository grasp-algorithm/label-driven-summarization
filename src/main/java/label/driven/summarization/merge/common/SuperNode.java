package label.driven.summarization.merge.common;

import label.driven.summarization.graph.Session;
import label.driven.summarization.grouping.GroupingImpl;
import oracle.pgx.api.VertexCollection;

/**
 *
 * This class represents the set of nodes which has been merged and represents a new entity called `SuperNode`.
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/4/18.
 */
public interface SuperNode {

    /**
     * Return a fresh identifier to the super-node
     * @return the id of the super-node
     */
    int getId();

    /**
     * This method return the grouping to which belongs.
     * @see GroupingImpl#computeGroupings(Session)
     * @return the number of the grouping to which belongs.
     */
    int getGrouping();

    /**
     * This method return the set of vertices on the original graph which belongs to the current super-node.
     * @return the set of nodes.
     */
    VertexCollection<Integer> getVertices();
}
