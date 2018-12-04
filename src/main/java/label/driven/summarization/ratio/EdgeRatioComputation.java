package label.driven.summarization.ratio;

import label.driven.summarization.graph.Session;
import label.driven.summarization.grouping.GroupingImpl;
import label.driven.summarization.merge.PrefixPropertyConstant;
import label.driven.summarization.schema.Schema;
import label.driven.summarization.util.EdgeRatioRow;
import label.driven.summarization.util.EdgeRatioRowImpl;
import oracle.pgql.lang.PgqlException;
import oracle.pgx.api.PgqlResultSet;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.PgxResult;
import oracle.pgx.api.PgxVertex;
import oracle.pgx.api.filter.EdgeFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Instead of calculate the A+ compatible for the first grouping, we take the
 * result from the label-driven grouping implemented
 *
 * @author LIRIS
 * @version 1.0
 * @see GroupingImpl#computeGroupings(Session) (label.driven.summarization.graph.Session)
 * @since 1.0 5/13/18.
 */
public class EdgeRatioComputation {
    private static final Logger LOG = LogManager.getLogger("GLOBAL");
    private Float theta;
    private Schema schema;
    private PgxGraph graph;

    public EdgeRatioComputation(PgxGraph graph, Schema schema) {
        this.graph = graph;
        this.schema = schema;
        this.theta = schema.getConfig().getTheta();
    }


    /**
     * This method compute the participation ratio (PA) for each grouping.
     * Check the paper `Efficient Aggregation for Graph Summarization`
     * http://pages.cs.wisc.edu/~jignesh/publ/summarization.pdf
     * <p>
     * if the PA between two grouping is:
     * > 0.5 -> They have a strong relationship
     * <= 0.5 -> They have a weak relationship
     * NaN | Inf -> No edge present in the grouping
     * <p>
     * Important: If we're using the label-driven grouping like an input to this method, the PA of each grouping to
     * itself should be 1, because of our approach.
     *
     * <equation>
     * TODO: A voir
     * relative_rechability = (\sigma )
     * </equation>
     * <p>
     * The participation ratio between G_{i} and G_{j} for a given label l is equals to the number of cross-edges with
     * label l from all the vertices in G_{i} to all the vertices in G{j} divided by the sum of edges with label l with
     * from G_{i}
     *
     * @return a map with the weight of the participation ratio
     */
    public Map<EdgeRatioRow, Float> computeProbabilityOfArriveTo() {
        Map<EdgeRatioRow, Float> participationRatioMap = new HashMap<>();
        Set<SuperNodePair> superNodePair = new HashSet<>();

        try {
            PgqlResultSet resultSet = graph.queryPgql(String.format("SELECT x.%1$s, y.%1$s, label(e) MATCH " +
                    "(x) -[e]-> (y) WHERE x.%1$s != y.%1$s", PrefixPropertyConstant.GROUPING.toString()));

            for (PgxResult result : resultSet)
                superNodePair.add(new SuperNodePair(result.getInteger(1), result.getInteger(2),
                        result.getString(3)));

        } catch (PgqlException | ExecutionException | InterruptedException e) {
            LOG.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

        superNodePair.forEach(p -> computeRatio(p.getSuperNodeI(), p.getSuperNodeJ(), p.getLabel(),
                participationRatioMap));

        LOG.debug("Computation of participation-ratio finished.");
        return participationRatioMap;
    }

    /**
     * @param groupingI
     * @param groupingJ
     * @param l
     * @param ratioMap
     */
    private void computeRatio(String groupingI, String groupingJ, String l, Map<EdgeRatioRow, Float> ratioMap) {
        try {

            String filterByLabel = "src.%1$s == %2$s && dst.%1$s == %3$s && edge.label() == '%4$s'";
//            String groupingFilter = "vertex.%1$s == %2$s";

//            if (!schema.isComputeConcatenationProperties()) {
                int edgesIJ = graph.getEdges(new EdgeFilter(String.format(filterByLabel, PrefixPropertyConstant.GROUPING, groupingI, groupingJ, l)))
                        .size();

                EdgeRatioRow pa = new EdgeRatioRowImpl(l, Integer.parseInt(groupingI),
                        Integer.parseInt(groupingJ), (float) edgesIJ, 1);
                ratioMap.put(pa, pa.getRatio());

//            } else {
//
//                VertexSet<Integer> sourceFrontierNodes = graph.filter(new EdgeFilter(String.format(filterByLabel,
//                        PrefixPropertyConstant.GROUPING.toString(), groupingI, groupingJ, l))
//                        .intersect(new VertexFilter(String.format(groupingFilter,
//                                PrefixPropertyConstant.GROUPING, groupingI)))).getVertices();
//
//                AtomicReference<Float> minimalNumberOfPaths = new AtomicReference<>(0F);
//                if (sourceFrontierNodes.size() > 0) {
//                    sourceFrontierNodes.forEach(v -> {
//                        try {
//                            computeDeltaInOut(v, groupingI, groupingJ, l, minimalNumberOfPaths);
//                        } catch (ExecutionException | InterruptedException e) {
//                            LOG.error(e.getMessage(), e);
//                            Thread.currentThread().interrupt();
//                        }
//                    });
//
//                    int edgesII = graph.filter(new EdgeFilter(String.format(filterByLabel, PrefixPropertyConstant.GROUPING, groupingI, groupingI, l)))
//                            .getEdges().size();
//
//                    int edgesIJ = graph.filter(new EdgeFilter(String.format(filterByLabel, PrefixPropertyConstant.GROUPING, groupingI, groupingJ, l)))
//                            .getEdges().size();
//
//                    long vjiTarget = graph.filter(new EdgeFilter(String.format(filterByLabel, PrefixPropertyConstant.GROUPING, groupingI, groupingJ, l))
//                            .intersect(new VertexFilter(String.format(groupingFilter,
//                                    PrefixPropertyConstant.GROUPING.toString(), groupingJ)))).getNumVertices();
//
//                    long vj = graph.filter(new VertexFilter(String.format(groupingFilter,
//                            PrefixPropertyConstant.GROUPING.toString(), groupingJ))).getNumVertices();
//
//                    EdgeRatioRow pa = new EdgeRatioRowImpl(l, Integer.parseInt(groupingI),
//                            Integer.parseInt(groupingJ), (float) edgesIJ);
//
//                    pa.computeRatio(minimalNumberOfPaths.get() * vjiTarget, Math.max(1, edgesII) * edgesIJ * sourceFrontierNodes.size() * (float) vj);
//
//                    if (!Float.isNaN(pa.getRatio()) &&
//                            !Float.isInfinite(pa.getRatio()) &&
//                            pa.getRatio() != 0f && pa.getRatio() > theta)
//                        ratioMap.put(pa, pa.getRatio());
//                }
//            }
        } catch (ExecutionException | InterruptedException e) {
            LOG.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * @param groupingI
     * @param l
     * @param minimalPath
     */
    private void computeDeltaInOut(PgxVertex<Integer> v, String groupingI, String groupingJ, String l,
                                   AtomicReference<Float> minimalPath) throws ExecutionException, InterruptedException {
        long deltaIn = 0L;
        long deltaOut = 0L;

        try {
            String filterLabelGrouping = "edge.label() == '%2$s' && src.%1$s == %3$s && dst.%1$s == %4$s";
            // out-degree of cross-edges
            deltaOut = graph.filter(new EdgeFilter(String.format(filterLabelGrouping,
                    PrefixPropertyConstant.GROUPING.toString(), l, groupingI, groupingJ))).getVertex(v.getId())
                    .getOutDegree();

            // in-degree of inner-edges
            deltaIn = graph.filter(new EdgeFilter(String.format(filterLabelGrouping,
                    PrefixPropertyConstant.GROUPING.toString(), l, groupingI, groupingI))).getVertex(v.getId())
                    .getInDegree();

        } catch (CompletionException e) {
            /* Just ignore this exception, it's possible that not value is found on the filter, which indicates that the
             * value of deltaIn will be 0 */
        } finally {
            minimalPath.accumulateAndGet((float) Math.max(1, deltaIn * deltaOut), (a, b) -> a + b);
        }
    }

}