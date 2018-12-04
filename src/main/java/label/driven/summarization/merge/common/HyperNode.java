package label.driven.summarization.merge.common;

import label.driven.summarization.graph.Session;
import label.driven.summarization.grouping.GroupingImpl;
import oracle.pgx.api.PgxVertex;

import java.util.Set;

/**
 *
 * This class represents the set of super-nodes which has been merged and represents a new entity
 * called `HyperNode`.
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/4/18.
 */
public interface HyperNode {

    /**
     * Return a fresh identifier to the hyper-node
     * @return the id of the hyper-node
     */
    int getId();

    /**
     * This method return the grouping to which belongs the super-nodes. However a hyper-node can be
     * composed of multiple super-node which belongs to a different grouping.
     * @see GroupingImpl#computeGroupings(Session)
     *
     * @return the number of the grouping to which belongs.
     */
    int getGrouping();

    /**
     * Returns the average of evaluation weight inside this hyper-node
     * @return the average of evaluation weight.
     */
    float getAvgWeight();

    /**
     * This method return the set of super-nodes which belongs to the current hyper-node.
     * @return the set of super-nodes.
     */
    Set<PgxVertex> getVertices();
}
