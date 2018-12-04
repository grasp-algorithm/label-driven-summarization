package label.driven.summarization.grouping;

import label.driven.summarization.graph.Session;

import java.util.Map;
import java.util.Set;

/**
 * This class implements the grouping using the `label-driven` strategy.
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 7/18/18.
 */
public interface Grouping {

    /**
     * This method compute the grouping by the frequency of labels.
     * At first, it retrieves the labels sorted by their frequency.
     * Then, for each label group in different groupings.
     * <p>
     * e.g.
     * Let's suppose :
     * v1 -e[:l1]-> v2
     * Both, v1 and v2 are going to the grouping 1
     * <p>
     * Attention, the grouping strategy is `Edge Cut`, the vertices on each grouping are disjoints.
     * e.g.
     * Let's suppose that we have:
     * v1 -e[:l1]-> v2
     * v1 -e[:l2]-> v2
     * v1 and v2 are going only to the grouping 1, even the second label is persisted in the grouping 1.
     *
     * @param session a graph with the pgx session
     * @return a map with the grouping number and the list of nodes in each one.
     */
    Map<Integer, Set<Integer>> computeGroupings(Session session);


    /**
     * Returns the grouping by the corresponding index of the label
     * @see this#getCorrespondenceGroupingLabel()
     *
     * @return the computed groupings
     */
    Map<Integer, Set<Integer>> getGroupings();

    /**
     * Returns a map with the correspondence between the labels on the input graph and an arbitrary index
     * used to the groupings.
     *
     * @return the map of correspondences
     */
    Map<Integer, String> getCorrespondenceGroupingLabel();
}
