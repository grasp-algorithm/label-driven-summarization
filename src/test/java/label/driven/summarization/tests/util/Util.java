package label.driven.summarization.tests.util;

import com.google.common.util.concurrent.AtomicDouble;
import label.driven.summarization.merge.Constants;
import label.driven.summarization.merge.PrefixPropertyConstant;
import oracle.pgql.lang.PgqlException;
import oracle.pgx.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 6/8/18.
 */
public class Util {
    private static final Logger LOG = LogManager.getLogger("GLOBAL");

    private Util() {
        throw new UnsupportedOperationException("This class couldn't be instantiated.");
    }

    /**
     * The sum of all the weights on the edges plus the the sum of all the properties which represents more edges.
     * @param graph the original graph
     * @return the sum of both values
     */
    public static double sumOfEdgesWeights(PgxGraph graph) {
        AtomicDouble sumOfEdgesWeights = new AtomicDouble();

        Set<EdgeProperty<?>> edgeProperties = graph.getEdgeProperties();
        edgeProperties.removeIf(ep -> !ep.getName().startsWith(PrefixPropertyConstant.EDGE_WEIGHT.toString()));

        graph.getEdges().forEach(edge -> {
            edgeProperties.forEach(prop -> {
                try {
                    sumOfEdgesWeights.addAndGet((double)(float) edge.getProperty(prop.getName()));
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        });

        AtomicLong sumOfAllPredicatesOccurrences = new AtomicLong();
        try {
            PgqlResultSet results = graph.queryPgql(String.format("SELECT SUM(x.%s) MATCH (x)",
                    PrefixPropertyConstant.NUMBER_OF_INNER_EDGES.toString()));
            sumOfAllPredicatesOccurrences.set((long)(double)results.iterator().next().getDouble(1));

        } catch (ExecutionException | InterruptedException | PgqlException e) {
            LOG.error(e.getMessage(), e);
        }

        return sumOfAllPredicatesOccurrences.get() + sumOfEdgesWeights.get();
    }


    public static long sumOfLabelsOccurrences(PgxGraph graph) {
        AtomicLong sumWeightsOnLabels = new AtomicLong(0L);
        Set<EdgeProperty<?>> edgeProperties = graph.getEdgeProperties();
        graph.getEdges().forEach(edge -> {
            edgeProperties.forEach(prop -> {
                try {
                    if (prop.getName().startsWith(PrefixPropertyConstant.EDGE_WEIGHT.toString()
                            .concat(Constants.SEPARATOR_PROPERTY))) {
                        if (edge.getProperty(prop.getName()).getClass().equals(Float.class))
                            sumWeightsOnLabels.addAndGet((long) (float) edge.getProperty(prop.getName()));
                        else if (edge.getProperty(prop.getName()).getClass().equals(Integer.class))
                            sumWeightsOnLabels.addAndGet((long) (int) edge.getProperty(prop.getName()));

                    }
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }

            });
        });

        return sumWeightsOnLabels.get();
    }


    public static void checkPercentageOnSN(PgxGraph graph) {
        Set<VertexProperty<?,?>> vertexProperties = graph.getVertexProperties();
        vertexProperties.removeIf(p -> !p.getName().startsWith(PrefixPropertyConstant.PERCENTAGE.toString()));

        AtomicReference<Float> sumPercentages = new AtomicReference<>();
        graph.getVertices().forEach(vertex -> {
            sumPercentages.set(0F);
            vertexProperties.forEach(prop -> {
                try {
                    sumPercentages.accumulateAndGet(vertex.getProperty(prop.getName()), (a,b) -> a+b);

                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            });

            assert (Math.round(sumPercentages.get()) == 1 || Math.round(sumPercentages.get()) == 0);
        });

    }

    public static void printGraph(PgxGraph graph) {
        System.out.println("VERTICES:: ");
        Set<VertexProperty<?,?>> properties = graph.getVertexProperties();
        graph.getVertices().forEach(v -> {
            System.out.println(v.getId());
            properties.forEach(prop -> {
                try {
                    System.out.println(prop.getName().concat(": ").concat(String.valueOf((((Float)v.getProperty(prop.getName())).floatValue()))));
                } catch(ClassCastException e) {
                    try {
                        System.out.println(prop.getName().concat(": ").concat(String.valueOf((((Integer)v.getProperty(prop.getName())).floatValue()))));
                    } catch (ExecutionException | InterruptedException e1) {
                        e1.printStackTrace();
                    }
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        });

        System.out.println("EDGES:: ");
        Set<EdgeProperty<?>> propEdges = graph.getEdgeProperties();
        graph.getEdges().forEach(e -> {
            System.out.print(e.getSource().getId() + " " + e.getLabel() + " " + e.getDestination().getId());
            propEdges.forEach(prop -> {
                try {
                    if (prop.getName().startsWith("EDGE"))
                    System.out.print(" " + prop.getName().concat(": ").concat(String.valueOf((((Float)e.getProperty(prop.getName())).floatValue()))));
                } catch (ExecutionException | InterruptedException e1) {
                    e1.printStackTrace();
                }
            });
            System.out.println();
        });
    }


    public static int getNumberOfLabels(PgxGraph graph) throws ExecutionException, InterruptedException {
        int counter = 0;

        PgqlResultSet results = graph.queryPgql("SELECT label(e) MATCH () -[e]-> () GROUP BY label(e)");
        for (PgxResult result: results)
            counter++;

        return counter;
    }
}
