package label.driven.summarization.util.output.format;

import label.driven.summarization.merge.common.HyperNode;
import oracle.pgx.api.PgxGraph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/12/18.
 */
public class GraphOutputFormatBuilder {
    private static final Logger LOG = LogManager.getLogger("GLOBAL");

    private PgxGraph graph;
    private PgxGraph summary;
    private Map<Integer, HyperNode> hyperNodes;
    private String outputPath;


    PgxGraph getGraph() {
        return graph;
    }

    PgxGraph getSummary() {
        return summary;
    }

    Map<Integer, HyperNode> getHyperNodes() {
        return hyperNodes;
    }

    String getOutputPath() {
        return outputPath;
    }

    public GraphOutputFormatBuilder setGraph(PgxGraph graph) {
        this.graph = graph;
        return this;
    }

    public GraphOutputFormatBuilder setSummary(PgxGraph summary) {
        this.summary = summary;
        return this;
    }

    public GraphOutputFormatBuilder setOutputPath(String outputPath) {
        this.outputPath = outputPath;
        return this;
    }

    public GraphOutputFormatBuilder setHyperNodes(Map<Integer, HyperNode> hyperNodes) {
        this.hyperNodes = hyperNodes;
        return this;
    }

    public void build() {
        try {
            (new GraphOutputFormat(this))
                    .writeSuperAndHyperNodesFile()
                    .writeOnlyHyperNodesFile()
                    .writeSuperEdgesFile()
                    .writeHyperEdgesFile();

        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }
}
