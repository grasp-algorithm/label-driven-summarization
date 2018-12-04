package label.driven.summarization.tests;

import com.google.common.util.concurrent.AtomicDouble;
import label.driven.summarization.exception.MergeStrategyException;
import label.driven.summarization.exception.SummaryGraphConstructionException;
import label.driven.summarization.graph.GraphSession;
import label.driven.summarization.grouping.Grouping;
import label.driven.summarization.grouping.GroupingImpl;
import label.driven.summarization.merge.PrefixPropertyConstant;
import label.driven.summarization.merge.evaluation.Evaluation;
import label.driven.summarization.merge.frequency.Frequency;
import label.driven.summarization.ratio.EdgeRatioComputation;
import label.driven.summarization.schema.Schema;
import label.driven.summarization.schema.SchemaBuilder;
import label.driven.summarization.tests.util.Util;
import label.driven.summarization.util.EdgeRatioRow;
import oracle.pgql.lang.PgqlException;
import oracle.pgx.api.PgqlResultSet;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.PgxResult;
import oracle.pgx.api.filter.EdgeFilter;
import oracle.pgx.api.filter.VertexFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/4/18.
 */
@DisplayName("SocialNetworkDatasetGraphTest")
class SocialNetworkDatasetGraphTest {

    private GraphSession graphSession;
    private Schema schema;
    private Grouping grouping;
    private PgxGraph summaryEvaluation;

    @BeforeEach
    void loadGraph() throws InterruptedException, ExecutionException, SummaryGraphConstructionException, IOException {
        URL schemaFile = getClass().getClassLoader().getResource("social_network_dataset/schema.json");
        URL edgeFile = getClass().getClassLoader().getResource("social_network_dataset/edge_file.txt");

        graphSession = new GraphSession();
        graphSession.initialize();

        schema = new SchemaBuilder()
                .fromSchemaFile(schemaFile.getPath())
                .build();

        graphSession.buildGraph(schema, edgeFile.getPath(),
                null);

        System.out.println("epsilon: " + schema.getConfig().getEpsilon());
        System.out.println("theta: " + schema.getConfig().getTheta());
    }


    private void grouping() throws ExecutionException, InterruptedException {

        System.out.println("Original graph ");
        System.out.println("Number of vertices: " + graphSession.getGraph().getNumVertices());
        System.out.println("Number of edges: " + graphSession.getGraph().getNumEdges());
        System.out.println("Isolated vertices: " + graphSession.getGraph().
                filter(new VertexFilter("vertex.outDegree() == 0 && vertex.inDegree() == 0"))
                .getNumVertices());

        grouping = new GroupingImpl();
        Map<Integer, Set<Integer>> groupings = grouping.computeGroupings(graphSession);

        /* The most frequent label is `l4`, so it should appear at the head of the list */
        assert (graphSession.getListOfLabels().get(0).equals("isLocatedIn"));

        Map<Integer, String> correspondence = grouping.getCorrespondenceGroupingLabel();
        /*  */
        assert (correspondence.entrySet().stream().filter(p -> p.getValue().equals("isLocatedIn")).findAny()
                .get().getKey().equals(correspondence.keySet().stream().mapToInt(k -> k).min().getAsInt()));

        graphSession.cleanGroupings();
        graphSession.updateGraph(groupings);

        /* The number of vertices without grouping should be 0. */
        long numberOfVerticesWithNoneGrouping = graphSession.getGraph().filter(new VertexFilter(String.format("vertex.%s = -1",
                PrefixPropertyConstant.GROUPING.toString()))).getNumVertices();
        assert (numberOfVerticesWithNoneGrouping == 0L);

        /* The number of isolated vertices should be equals to the number of grouping minus the number of frequent labels */
        Integer maxGroupingId = grouping.getCorrespondenceGroupingLabel().keySet().stream().mapToInt(i -> i).max()
                .orElse(-1);
        long numberOfVerticesIsolated = groupings.entrySet().stream().filter(p -> p.getKey() > maxGroupingId && p.getValue().size() == 1).count();
        assert (numberOfVerticesIsolated == (groupings.keySet().stream().mapToInt(k -> k).max().getAsInt() - maxGroupingId));

        /* the number of groupings should be `10`*/
        assert (groupings.size() == 10);
    }

    private void evaluation() throws ExecutionException, InterruptedException, SummaryGraphConstructionException, MergeStrategyException, PgqlException {
        grouping();

        /* Computation of Super-nodes */
        Evaluation evaluation = new Evaluation(schema, graphSession);
        evaluation.evaluate(grouping.getGroupings().keySet(), grouping.getCorrespondenceGroupingLabel());

        AtomicInteger sum = new AtomicInteger(0);
        evaluation.getSuperNodes().forEach((k, v) -> sum.addAndGet(v.getVertices().size()));

        graphSession.cleanGroupings();
        graphSession.updateGraphGroupings(evaluation.getSuperNodes());

        /* The number of vertices without grouping should be 0. */
        long numberOfVerticesWithNoneGrouping = graphSession.getGraph().filter(new VertexFilter(String.format("vertex.%s = -1",
                PrefixPropertyConstant.GROUPING.toString()))).getNumVertices();
        assert (numberOfVerticesWithNoneGrouping == 0L);

        EdgeRatioComputation edgeRatioComputation = new EdgeRatioComputation(graphSession.getGraph(), schema);
        Map<EdgeRatioRow, Float> pa = edgeRatioComputation.computeProbabilityOfArriveTo();

        /* all the edge ratios should be between 0 and 1 */
        long numberOfPAGreaterThanOne = pa.entrySet().stream().filter(p -> p.getValue() > 1).count();
        assert (numberOfPAGreaterThanOne == 0L);

        long numberOfMultiLabels = pa.entrySet().stream().filter(p1 -> pa.entrySet().stream()
                .filter(p2 -> p2.equals(p1)).count() > 1).count();
        assert (numberOfMultiLabels == 0L);

        summaryEvaluation = evaluation.computeEdges(pa);
        System.out.println("Evaluation summary ");
        System.out.println("Number of vertices: " + summaryEvaluation.getNumVertices());
        System.out.println("Number of edges: " + summaryEvaluation.getNumEdges());
        System.out.println("Isolated vertices: " + summaryEvaluation.
                filter(new VertexFilter("vertex.outDegree() == 0 && vertex.inDegree() == 0"))
                .getNumVertices());

        long numberOfPAEqualsToOne = pa.entrySet().stream().filter(p -> p.getValue() == 1).count();
        System.out.println("Percentage of 1: " + (float) numberOfPAEqualsToOne / pa.size());

        Util.checkPercentageOnSN(summaryEvaluation);

//        assert (summaryEvaluation.getNumVertices() == 105);
//        assert (summaryEvaluation.getNumEdges() == 266);
//        assert (summaryEvaluation.filter(new VertexFilter("vertex.outDegree() == 0 && " +
//                "vertex.inDegree() == 0")).getNumVertices() == 0);

        //Util.printGraph(summaryEvaluation);
        System.out.println(Util.sumOfEdgesWeights(summaryEvaluation));
        System.out.println(graphSession.getGraph().getNumEdges());
        assert (Util.sumOfEdgesWeights(summaryEvaluation) == graphSession.getGraph().getNumEdges());
    }

    @Test
    void frequency() throws ExecutionException, InterruptedException, SummaryGraphConstructionException, MergeStrategyException, PgqlException {
        evaluation();

        Frequency frequency = new Frequency(graphSession, summaryEvaluation, schema);
        PgxGraph summaryFrequency = frequency
                .mergeByFrequency()
                .calibrateEdges();

        System.out.println("Frequency summary ");
        System.out.println("Number of vertices: " + summaryFrequency.getNumVertices());
        System.out.println("Number of edges: " + summaryFrequency.getNumEdges());
        System.out.println("Isolated vertices: " + summaryFrequency.
                filter(new VertexFilter("vertex.outDegree() == 0 && vertex.inDegree() == 0"))
                .getNumVertices());

//        assert (summaryFrequency.getNumVertices() == 43);
//        assert (summaryFrequency.getNumEdges() == 242);

        Util.checkPercentageOnSN(summaryFrequency);

        /* We verify that the sum of all the NODE_WEIGHT_EVALUATION's properties is equals to the number of vertices in the
        original graph */
        AtomicDouble sumOfNodeWeights = new AtomicDouble();
        PgqlResultSet results = summaryFrequency.queryPgql(String.format("SELECT SUM(n.%s) MATCH (n)", PrefixPropertyConstant.NODE_WEIGHT_EVALUATION.toString()));
        for (PgxResult result : results) {
            sumOfNodeWeights.set(result.getDouble(1));
        }

        assert (sumOfNodeWeights.get() == graphSession.getGraph().getNumVertices());

        System.out.println("Percentage of 1 ratio: " + (float) summaryFrequency
                .filter(new EdgeFilter("edge.EDGE_RATIO == 1")).getNumEdges() / summaryFrequency.getNumEdges());

        //Util.printGraph(newSummary);

        /* This verify that the sum of weights-edges in the summary + the sum of the value of the properties
        `PREDICATES` is equals to the number of edge in the original graph */
        assert (Util.sumOfEdgesWeights(summaryFrequency) == graphSession.getGraph().getNumEdges());
    }

//    @Test
//    void clustering() throws InterruptedException, ExecutionException, PgqlException, MergeStrategyException, SummaryGraphConstructionException {
//        frequency();
//        Clustering clustering = new Clustering(schema, Util.getNumberOfLabels(summaryFrequency));
//        PgxGraph summaryClustering = clustering.clusterEdges(summaryFrequency);
//
//        System.out.println("Clustering merge ");
//        System.out.println("Number of edges: " + summaryClustering.getNumEdges());
//
//        assert (summaryClustering.getNumEdges() == 79);
//        assert (Util.sumOfEdgesWeights(summaryClustering) == 158);
//        assert (Util.sumOfLabelsOccurrences(summaryClustering) == 116);
//        Util.printGraph(summaryClustering);
//
//    }


    @AfterEach
    void closeSession() throws ExecutionException, InterruptedException {
        graphSession.getGraph().destroy();
        graphSession.close();
    }
}
