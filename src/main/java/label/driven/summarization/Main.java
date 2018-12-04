package label.driven.summarization;

import label.driven.summarization.graph.GraphSession;
import label.driven.summarization.graph.Session;
import label.driven.summarization.grouping.Grouping;
import label.driven.summarization.grouping.GroupingImpl;
import label.driven.summarization.merge.Constants;
import label.driven.summarization.merge.PrefixPropertyConstant;
import label.driven.summarization.merge.evaluation.Evaluation;
import label.driven.summarization.merge.frequency.Frequency;
import label.driven.summarization.ratio.EdgeRatioComputation;
import label.driven.summarization.schema.Schema;
import label.driven.summarization.schema.SchemaBuilder;
import label.driven.summarization.util.EdgeRatioRow;
import label.driven.summarization.util.output.format.GraphOutputFormatBuilder;
import oracle.pgql.lang.PgqlException;
import oracle.pgx.api.EdgeProperty;
import oracle.pgx.api.PgqlResultSet;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.PgxResult;
import oracle.pgx.config.FileGraphConfig;
import oracle.pgx.config.Format;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class Main {
    private static final Logger LOG = LogManager.getLogger("GLOBAL");

    public static void main(String[] args) throws Exception {
        System.out.println("Starting ... ");

        long start = System.currentTimeMillis();

        Session session = new GraphSession();
        session.initialize();

        //"executed_tests/edge_file.txt"
        //"executed_tests/vertex_file.txt"
        //"executed_tests/schema.json"

//         args
        String edgeFile = args[0] + "/edge_file.txt";
        String verticesFile = args[0] + "/vertex_file.txt";
        String schemaFile = args[0] + "/schema.json";

        Schema schema = new SchemaBuilder()
                .fromSchemaFile(schemaFile)
                .build();

        session.buildGraph(schema, edgeFile, verticesFile);

//        String schemaFile = "~/label-driven-summarization/src/" +
//                "main/resources/summaries/running-example/schema.json";
//        Schema schema = new SchemaBuilder()
//                .fromSchemaFile(schemaFile)
//                .build();
//        session.buildGraph("~/label-driven-summarization/src/" +
//                "main/resources/summaries/running-example/example-graph.json");

        printResults("ORIGINAL GRAPH", session.getGraph(), null);
        System.out.println("Graph loaded in " + (System.currentTimeMillis() - start) + " ms.");

        start = System.currentTimeMillis();

        Grouping grouping = new GroupingImpl();
        Map<Integer, Set<Integer>> groupings = grouping.computeGroupings(session);

        long groupingTime = System.currentTimeMillis() - start;
        System.out.println("Grouping is finished. " + groupingTime + " ms");

        session.cleanGroupings();
        session.updateGraph(groupings);

        System.out.println("Starting evaluation.");
        Evaluation evaluation = new Evaluation(schema, session);
        evaluation.evaluate(groupings.keySet(), grouping.getCorrespondenceGroupingLabel());

        /* We don't need more this */
        groupings.clear();
        grouping.getCorrespondenceGroupingLabel().clear();

        System.out.println("Updating groupings");
        session.updateGraphGroupings(evaluation.getSuperNodes());

        System.out.println("Computing participation ratio. ");
        EdgeRatioComputation edgeRatioComputation = new EdgeRatioComputation(session.getGraph(), schema);
        Map<EdgeRatioRow, Float> pa = edgeRatioComputation.computeProbabilityOfArriveTo();
        System.out.println("End of computation of participation ratio. ");

        System.out.println("Starting the computation of super-edges.");
        PgxGraph evalSummary = evaluation.computeEdges(pa);
        long newStart = System.currentTimeMillis();
        printResults("AFTER THE EVALUATION", evalSummary, newStart - start);

        System.out.println("Starting frequency.");
        Frequency frequency = new Frequency(session, evalSummary, schema);
        frequency.mergeByFrequency();

        System.out.println("Computing the calibration of edges");
        PgxGraph frequencySummary = frequency.calibrateEdges();

        newStart = System.currentTimeMillis();
        printResults("AFTER THE FREQUENCY STEP", frequencySummary, newStart - start);

        new GraphOutputFormatBuilder()
                .setGraph(evalSummary)
                .setSummary(frequencySummary)
                .setHyperNodes(frequency.getHyperNodes())
                .setOutputPath(schemaFile.substring(0, schemaFile.lastIndexOf('/')).concat(Path.SEPARATOR))
                .build();

        long runtime = System.currentTimeMillis() - start;

        writeSummaryOnText(args[0], frequencySummary, "summary");
        writeStaticUseOfLabels(args[0], frequencySummary);

        printFinalResults(session.getGraph(), frequencySummary, runtime);
        session.close();
    }


    /**
     * This methods write an output file with all the labels and properties of vertices and edges
     *
     * @param summary the summary graph
     *
     * @throws ExecutionException   if the access to the properties fails
     * @throws InterruptedException if the access to the properties fails
     * @throws IOException          if the file cannot be written
     */
    private static void writeSummaryOnText(String outputPath, PgxGraph summary, String name) throws ExecutionException, InterruptedException, IOException {
        String configFileName = "/summary-pgx.json";
        String outputPathStore = outputPath + Path.SEPARATOR;

        FileGraphConfig config = summary.store(Format.EDGE_LIST, outputPathStore + "/summary-pgx.edgelist", true);
        if (Paths.get(outputPathStore + configFileName).toFile().exists())
            Files.delete(Paths.get(outputPathStore + configFileName));

        FileUtils.write(new File(outputPathStore + configFileName), config.toString(), Charset.defaultCharset());

        String output = outputPathStore + Path.SEPARATOR + name + ".txt";

        Set<EdgeProperty<?>> edgesProperties = summary.getEdgeProperties();

        // Changement du format
        if (Paths.get(output).toFile().exists())
            Files.delete(Paths.get(output));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output, true))) {
            summary.getVertices().forEach(v -> {
                try {
                    writer.append(String.valueOf((int) v.getId()).concat(" "));
                    summary.getVertexProperties().forEach(vp -> {
                        try {
                            if (v.getProperty(vp.getName()).getClass().equals(Float.class))
                                writer.append(vp.getName().concat(" ").concat(String.valueOf((float)
                                        v.getProperty(vp.getName()))).concat(" "));
                            else if (v.getProperty(vp.getName()).getClass().equals(Integer.class))
                                writer.append(vp.getName().concat(" ").concat(String.valueOf((int)
                                        v.getProperty(vp.getName()))).concat(" "));
                            else if (v.getProperty(vp.getName()).getClass().equals(Boolean.class))
                                writer.append(vp.getName().concat(" ").concat(String.valueOf((boolean)
                                        v.getProperty(vp.getName()))).concat(" "));

                        } catch (ExecutionException | InterruptedException | IOException e) {
                            LOG.error(e.getMessage(), e);
                            Thread.currentThread().interrupt();
                        }
                    });

                    writer.append("OUT_DEGREE ".concat(String.valueOf(v.getOutDegree())).concat(" "));
                    writer.append("IN_DEGREE ".concat(String.valueOf(v.getInDegree())).concat(" "));
                    writer.append("\n");
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            });

            summary.getEdges().forEach(ed -> {
                try {
                    writer.append(String.valueOf(ed.getSource().getId()).concat(" "));
                    writer.append(ed.getLabel().concat(" "));
                    writer.append(String.valueOf(ed.getDestination().getId()).concat(" "));

                    edgesProperties.forEach(ep -> {
                        try {

                            if (ed.getProperty(ep.getName()).getClass().equals(Float.class))
                                writer.append(ep.getName().concat(" ")
                                        .concat(String.valueOf((float) ed.getProperty(ep.getName()))).concat(" "));
                            else if (ed.getProperty(ep.getName()).getClass().equals(Integer.class))
                                writer.append(ep.getName().concat(" ")
                                        .concat(String.valueOf((int) ed.getProperty(ep.getName()))).concat(" "));
                        } catch (ExecutionException | InterruptedException | IOException e) {
                            LOG.error(e.getMessage(), e);
                            Thread.currentThread().interrupt();
                        }
                    });

                    writer.append("\n");
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            });
        }
    }


    /**
     * @param outputPath
     * @param graph
     *
     * @throws IOException
     */
    private static void writeStaticUseOfLabels(String outputPath, PgxGraph graph) throws IOException {
        String output = outputPath + Path.SEPARATOR + "static-label-use.txt";

        if (Paths.get(output).toFile().exists())
            Files.delete(Paths.get(output));

        Set<String> labelsOnHN = new HashSet<>();
        graph.getVertexProperties().forEach(vp -> {
            if (vp.getName().startsWith(PrefixPropertyConstant.PERCENTAGE.toString()))
                labelsOnHN.add(vp.getName().replace(PrefixPropertyConstant.PERCENTAGE.toString()
                        .concat(Constants.SEPARATOR_PROPERTY), ""));
        });

        Set<String> labelsOnCrossEdges = new HashSet<>();
        try {
            PgqlResultSet results = graph.queryPgql("SELECT label(e) MATCH () -[e]-> ()");
            for (PgxResult result : results)
                labelsOnCrossEdges.add(result.getString(1));

        } catch (ExecutionException | InterruptedException | PgqlException e) {
            LOG.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

        Set<String> labelsOnPathOut = new HashSet<>();
        graph.getVertexProperties().forEach(vp -> {
            if (vp.getName().startsWith(PrefixPropertyConstant.PATH_OUT.toString()))
                labelsOnPathOut.add(vp.getName().replace(PrefixPropertyConstant.PATH_OUT.toString()
                        .concat(Constants.SEPARATOR_PROPERTY), ""));
        });


        Set<String> labelsOnPathIn = new HashSet<>();
        graph.getVertexProperties().forEach(vp -> {
            if (vp.getName().startsWith(PrefixPropertyConstant.PATH_IN.toString()))
                labelsOnPathIn.add(vp.getName().replace(PrefixPropertyConstant.PATH_IN.toString()
                        .concat(Constants.SEPARATOR_PROPERTY), ""));
        });

        Set<String> labelsOnReachCount = new HashSet<>();
        graph.getVertexProperties().forEach(vp -> {
            if (vp.getName().startsWith(PrefixPropertyConstant.REACHABILITY_COUNT.toString()))
                labelsOnReachCount.add(vp.getName().replace(PrefixPropertyConstant.REACHABILITY_COUNT.toString()
                        .concat(Constants.SEPARATOR_PROPERTY), ""));
        });

        Set<String> sumInDegreesByLabel = new HashSet<>();
        graph.getVertexProperties().forEach(vp -> {
            if (vp.getName().startsWith(PrefixPropertyConstant.PARTICIPATION_LABEL.toString()) ||
                    vp.getName().startsWith(PrefixPropertyConstant.TRAVERSAL_FRONTIERS.toString()))
                sumInDegreesByLabel.add(vp.getName());
        });


        Set<String> listOfAllLabels = new HashSet<>(labelsOnHN);
        listOfAllLabels.addAll(labelsOnCrossEdges);
        listOfAllLabels.addAll(labelsOnReachCount);
        listOfAllLabels.addAll(labelsOnPathIn);
        listOfAllLabels.addAll(labelsOnPathOut);
        listOfAllLabels.addAll(sumInDegreesByLabel);


        /* The format of the output file is :
         * label <1 if it's present in HN properties, 0 otherwise> <1 if it's present in a CE label, 0 otherwise>
         * */
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output, true))) {
            writer.append("label HN CE RC PI PO SI SO");
            writer.newLine();

            for (String label : listOfAllLabels) {
                writer.append(label).append(" ");

                if (labelsOnHN.stream().anyMatch(l -> l.equals(label))) writer.append("1 ");
                else writer.append("0 ");

                if (labelsOnCrossEdges.stream().anyMatch(l -> l.equals(label))) writer.append("1 ");
                else writer.append("0 ");

                if (labelsOnReachCount.stream().anyMatch(l -> l.equals(label))) writer.append("1 ");
                else writer.append("0 ");

                if (labelsOnPathIn.stream().anyMatch(l -> l.equals(label))) writer.append("1 ");
                else writer.append("0 ");

                if (labelsOnPathOut.stream().anyMatch(l -> l.equals(label))) writer.append("1 ");
                else writer.append("0 ");

                //fixme
                if (sumInDegreesByLabel.stream().anyMatch(l -> l.equals(label))) writer.append("1 ");
                else writer.append("0 ");

                if (sumInDegreesByLabel.stream().anyMatch(l -> l.equals(label))) writer.append("1");
                else writer.append("0");


                writer.newLine();
            }
        }
    }


    private static void printFinalResults(PgxGraph originalGraph, PgxGraph summaryGraph, Long runtime) throws ExecutionException, InterruptedException, PgqlException {

        PgqlResultSet results = originalGraph.queryPgql("SELECT COUNT(DISTINCT label(e)) MATCH () -[e]-> ()");
        long labels = results.iterator().next().getLong(1);
        long verticesOG = originalGraph.getNumVertices();
        long edgesOG = originalGraph.getNumEdges();

        long verticesSG = summaryGraph.getNumVertices();
        long edgesSG = summaryGraph.getNumEdges();

        long properties = summaryGraph.getVertexProperties().size();
        float crVertices = 1F - ((float) verticesSG / verticesOG);
        float crEdges = 1F - ((float) edgesSG / edgesOG);

        System.out.println("labels verticesOG edgesOG verticesSG edgesSG runtime properties crVertices crEdges");
        System.out.println(String.format("%s %s %s %s %s %s %s %s %s", labels, verticesOG, edgesOG, verticesSG, edgesSG,
                runtime, properties, crVertices, crEdges));

    }

    /**
     * This methods prints the results of each summary
     *
     * @param title the title of the summary
     * @param graph the summary graph
     * @param time  the time to print
     *
     * @throws ExecutionException   if the filter fails
     * @throws InterruptedException if the filter fails
     */
    private static void printResults(String title, PgxGraph graph, Long time) throws ExecutionException, InterruptedException, PgqlException {
        LOG.debug("==================================");
        LOG.debug(title);
        LOG.debug("----------------------------------");
        LOG.debug("Edges: " + graph.getNumEdges());
        LOG.debug("Vertices: " + graph.getNumVertices());

//        System.out.println("Isolated: " +
//                graph.filter(new VertexFilter("vertex.inDegree() == 0 && vertex.outDegree() == 0")).getNumVertices());
//        System.out.println("Sources: " +
//                graph.filter(new VertexFilter("vertex.inDegree() == 0")).getNumVertices());
//        System.out.println("Sink: " +
//                graph.filter(new VertexFilter("vertex.outDegree() == 0")).getNumVertices());
        if (time != null)
            LOG.debug("Time: " + time + " ms");
        LOG.debug("==================================");
    }
}

