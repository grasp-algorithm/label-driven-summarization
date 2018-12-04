package label.driven.summarization.merge.frequency.heuristics;

import label.driven.summarization.merge.PrefixPropertyConstant;
import label.driven.summarization.schema.Schema;
import label.driven.summarization.schema.json.VertexMergeStrategy;
import oracle.pgql.lang.PgqlException;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.PgxVertex;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/27/18.
 */
interface MergeHeuristic {

    /**
     *
     * @param schema
     * @param graph
     *
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws PgqlException
     */
    List<Set<PgxVertex>> mergeSinkVertices(Schema schema, PgxGraph graph) throws ExecutionException, InterruptedException,
            PgqlException;

    /**
     *
     * @param graph
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws PgqlException
     */
    List<Set<PgxVertex>> mergeSourceAndIntermediateVertices(Schema schema, PgxGraph graph) throws ExecutionException, InterruptedException, PgqlException;


    default String builtQueryByRangeBySchemaConfig(Schema schema, Integer lowerLimit) {
        Integer upperLimit = lowerLimit + schema.getConfig().getEpsilon();

        String select = "SELECT sn ";
        String where = "WHERE out_degree(sn) > 0 AND ";

        String stmt = "";
        if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.WEIGHT_MERGE)) {
            stmt += String.format("sn.%1$s >= %2$s AND sn.%1$s <= %3$s",
                    PrefixPropertyConstant.NODE_WEIGHT_EVALUATION.toString(), lowerLimit, upperLimit);
        } else if (schema.getConfig().getMergingProcedure().contains(VertexMergeStrategy.INNER_MERGE_EQUALITY)) {
            stmt += String.format("sn.%1$s >= %2$s AND sn.%1$s <= %3$s",
                    PrefixPropertyConstant.REACHABILITY_COUNT.toString(), lowerLimit, upperLimit);
        }

        return select + " MATCH(sn) " + where + stmt;
    }
}
