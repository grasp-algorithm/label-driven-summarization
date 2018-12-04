package label.driven.summarization.util.output.format;

import label.driven.summarization.merge.Constants;
import label.driven.summarization.merge.PrefixPropertyConstant;
import label.driven.summarization.merge.common.HyperNode;
import oracle.pgx.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * TODO DOC
 *
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/12/18.
 */
class GraphOutputFormat {
    private static final Logger LOG = LogManager.getLogger("GLOBAL");

    private static final String SEPARATOR_FIELD = " ";
    private static final String SUPER_NODES_FILE_NAME = "super_nodes.txt";
    private static final String HYPER_NODES_FILE_NAME = "hyper_nodes.txt";
    private static final String SUPER_EDGES_FILE_NAME = "super_edges.txt";
    private static final String HYPER_EDGES_FILE_NAME = "hyper_edges.txt";

    private PgxGraph graph;
    private PgxGraph summary;
    private Map<Integer, HyperNode> hyperNodes;

    private String outputPath;
    private static final String FIRST_LINE_SUPER_NODES = "sn_id hn_id sn_weight hn_weight";
    private static final String FIRST_LINE_HYPER_NODES = "hn_id hn_weight";
    private static final String FIRST_LINE_SUPER_EDGES = "sn_src_id sn_dst_id ratio weight label";
    private static final String FIRST_LINE_HYPER_EDGES = "hn_src_id hn_dst_id ratio weight label";


    GraphOutputFormat(GraphOutputFormatBuilder builder) {
        this.graph = builder.getGraph();
        this.summary = builder.getSummary();
        this.hyperNodes = builder.getHyperNodes();
        this.outputPath = builder.getOutputPath();
    }

    /**
     * Vertices file
     * node_id node label hyper_node_id super_node_id Etc.. (weight, for each hyper/super node the cardinality, etc.
     * or any other attribute)
     *
     * @throws IOException if it's not possible write on the file.
     */
    GraphOutputFormat writeSuperAndHyperNodesFile() throws IOException {

        String outputVertices = outputPath + SUPER_NODES_FILE_NAME;

        if (Paths.get(outputVertices).toFile().exists())
            Files.delete(Paths.get(outputVertices));

        Set<VertexProperty<?,?>> properties = graph.getVertexProperties();
        properties.removeIf(prop -> !prop.getName().startsWith(PrefixPropertyConstant.PERCENTAGE.toString()));

        List<String> props = new ArrayList<>();
        properties.forEach(prop -> props.add(prop.getName()));

        String listOfLabels = String.join(" ", props.stream().map(val -> val
                .replace(PrefixPropertyConstant.PERCENTAGE.toString().concat(Constants.SEPARATOR_PROPERTY),
                        "")).collect(Collectors.toList()));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputVertices, true))) {
                writer.append(FIRST_LINE_SUPER_NODES.concat(SEPARATOR_FIELD).concat(listOfLabels));
                writer.newLine();

            for (PgxVertex sn : graph.getVertices()) {

                //snId
                writer.append(String.valueOf((int) sn.getId()).concat(SEPARATOR_FIELD));

                AtomicInteger hnId = new AtomicInteger(-1);
                hyperNodes.entrySet().stream().filter(superNode -> superNode.getValue().getVertices().stream()
                        .anyMatch(sv -> sv.getId().equals(sn.getId()))).findAny()
                        .ifPresent(superNode -> hnId.set(superNode.getKey()));
                //hnId
                writer.append(String.valueOf(hnId).concat(SEPARATOR_FIELD));


                // sn_weight
                Integer snWeight = (int)(float) sn.getProperty(PrefixPropertyConstant.NODE_WEIGHT_EVALUATION.toString());
                writer.append(String.valueOf(snWeight)).append(SEPARATOR_FIELD);

                // hn_weight
                int hnWeight = hyperNodes.get(hnId.get()).getVertices().size();
                writer.append(String.valueOf(hnWeight).concat(SEPARATOR_FIELD));

                int counter = 0;
                for (String prop: props) {
                    DecimalFormat df = new DecimalFormat("#.###");
                    df.setRoundingMode(RoundingMode.CEILING);
                    writer.append(df.format(((Float)sn.getProperty(prop))));

                    if (++counter != props.size())
                        writer.append(SEPARATOR_FIELD);
                }

                writer.newLine();
            }

        } catch ( InterruptedException | ExecutionException e) {
            LOG.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

        return this;
    }

    GraphOutputFormat writeOnlyHyperNodesFile() throws IOException {

        String outputVertices = outputPath + HYPER_NODES_FILE_NAME;

        if (Paths.get(outputVertices).toFile().exists())
            Files.delete(Paths.get(outputVertices));

        Set<VertexProperty<?,?>> properties = graph.getVertexProperties();
        properties.removeIf(prop -> !prop.getName().startsWith(PrefixPropertyConstant.PERCENTAGE.toString()));

        List<String> props = new ArrayList<>();
        properties.forEach(prop -> props.add(prop.getName()));

        String listOfLabels = String.join(" ", props.stream().map(val -> val
                .replace(PrefixPropertyConstant.PERCENTAGE.toString().concat(Constants.SEPARATOR_PROPERTY),
                        "")).collect(Collectors.toList()));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputVertices, true))) {
            writer.append(FIRST_LINE_HYPER_NODES.concat(SEPARATOR_FIELD).concat(listOfLabels));
            writer.newLine();

            for (PgxVertex hn : summary.getVertices()) {
                //hnId
                writer.append(String.valueOf((int)hn.getId())).append(SEPARATOR_FIELD);

                // hn_weight
                writer.append(String.valueOf((int)hn.getProperty(PrefixPropertyConstant.
                        NODE_WEIGHT_FREQUENCY.toString()))).append(SEPARATOR_FIELD);

                int counter = 0;
                for (String prop: props) {
                    DecimalFormat df = new DecimalFormat("#.###");

                    df.setRoundingMode(RoundingMode.CEILING);
                    writer.append(df.format(((Float)hn.getProperty(prop))));

                    if (++counter != props.size())
                        writer.append(SEPARATOR_FIELD);
                }

                writer.newLine();
            }

        } catch ( InterruptedException | ExecutionException e) {
            LOG.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

        return this;

    }


    /**
     * Format of the output file:
     * sn_id_src sn_id_dst ratio weight label
     *
     * @throws IOException if it's not possible write on the file.
     */
    GraphOutputFormat writeSuperEdgesFile() throws IOException {
        writeEdgeFile(graph, SUPER_EDGES_FILE_NAME, FIRST_LINE_SUPER_EDGES);
        return this;
    }

    /**
     * Format of the output file:
     * sn_id_src sn_id_dst ratio weight label
     *
     * @throws IOException if it's not possible write on the file.
     */
    void writeHyperEdgesFile() throws IOException {
        writeEdgeFile(summary, HYPER_EDGES_FILE_NAME, FIRST_LINE_HYPER_EDGES);
    }


    private void writeEdgeFile(PgxGraph graph, String fileName, String firstLine) throws IOException {
        String outputEdges = outputPath + fileName;

        if (Paths.get(outputEdges).toFile().exists())
            Files.delete(Paths.get(outputEdges));

        Set<EdgeProperty<?>> properties = graph.getEdgeProperties();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputEdges, true))) {
            writer.append(firstLine.concat("\n"));

            for (PgxEdge edge : graph.getEdges()) {

                writer.append(String.valueOf(edge.getSource().getId()).concat(SEPARATOR_FIELD));
                writer.append(String.valueOf(edge.getDestination().getId()).concat(SEPARATOR_FIELD));

                DecimalFormat df = new DecimalFormat("#.###");

                df.setRoundingMode(RoundingMode.CEILING);

                writer.append(String.valueOf(df.format((Float) edge.getProperty(PrefixPropertyConstant.EDGE_RATIO.toString())))
                        .concat(SEPARATOR_FIELD));
                writer.append(String.valueOf((Float) edge.getProperty(PrefixPropertyConstant.EDGE_WEIGHT.toString()))
                        .concat(SEPARATOR_FIELD));

                if (edge.getLabel().length() > 0)
                    writer.append(edge.getLabel().concat(SEPARATOR_FIELD));

                for (EdgeProperty prop: properties) {
                    if (prop.getName().startsWith("l") && edge.getProperty(prop.getName()).equals(1))
                        writer.append(prop.getName().concat(SEPARATOR_FIELD));
                }

                writer.append("\n");
            }

        } catch (IOException | InterruptedException | ExecutionException e) {
            LOG.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

}
