package label.driven.summarization.merge.evaluation.histogram.mutation;

import label.driven.summarization.graph.Session;
import label.driven.summarization.merge.Constants;
import label.driven.summarization.merge.PrefixPropertyConstant;
import oracle.pgx.api.*;
import oracle.pgx.config.IdGenerationStrategy;

import java.util.Collection;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 7/18/18.
 */
public class FilterImpl implements Filter, AutoCloseable {

    private PgxGraph filteredGraph;

    @Override
    public PgxGraph apply(Session session, VertexCollection<Integer> innerVertices, int grouping)
            throws ExecutionException, InterruptedException {

        GraphBuilder<Integer> builder = session.getSession().createGraphBuilder(IdGenerationStrategy.USER_IDS, IdGenerationStrategy.USER_IDS);

        for (PgxVertex<Integer> vertex : innerVertices) {

            session.getGraph().getVertex(vertex.getId()).getOutEdges().stream()
                    .filter(e -> {
                        try {
                            return innerVertices.contains(e.getDestination());
                        } catch (CompletionException ex) {
                            // it means that doesn't exists the id in the collection
                            // why PGX ? :(
                            return false;
                        }
                    })
                    .forEach(e -> {

                        VertexBuilder<Integer> src = builder.addVertex((Integer) e.getSource().getId())
                                .setProperty(PrefixPropertyConstant.GROUPING.toString(), grouping);

                        VertexBuilder<Integer> dst = builder.addVertex((Integer) e.getDestination().getId())
                                .setProperty(PrefixPropertyConstant.GROUPING.toString(), grouping);

                        builder.addEdge(e.getId(), src, dst)
                                .setLabel(e.getLabel());
                    });


            session.getGraph().getVertex(vertex.getId()).getInEdges().stream()
                    .filter(e -> {
                        try {
                            return innerVertices.contains(e.getSource());
                        } catch (CompletionException ex) {
                            // it means that doesn't exists the id in the collection
                            // why PGX ? :(
                            return false;
                        }
                    })
                    .forEach(e -> {

                        VertexBuilder<Integer> src = builder.addVertex((Integer) e.getSource().getId())
                                .setProperty(PrefixPropertyConstant.GROUPING.toString(), grouping);

                        VertexBuilder<Integer> dst = builder.addVertex((Integer) e.getDestination().getId())
                                .setProperty(PrefixPropertyConstant.GROUPING.toString(), grouping);

                        builder.addEdge(e.getId(), src, dst)
                                .setLabel(e.getLabel());
                    });

        }

        filteredGraph = builder.build();
        return filteredGraph;
    }


    @Override
    public PgxGraph apply(Session session, PgxGraph graph, VertexCollection<Integer> innerVertices,
                          VertexCollection<Integer> vertexCollection, int grouping, boolean block)
            throws ExecutionException, InterruptedException {

        GraphBuilder<Integer> builder = session.getSession().createGraphBuilder(IdGenerationStrategy.USER_IDS, IdGenerationStrategy.USER_IDS);

        try (VertexSet<Integer> innerVerticesFiltered = graph.createVertexSet()) {
            vertexCollection.forEach(vc -> {
                try {
                    if (innerVertices.contains(vc))
                        innerVerticesFiltered.add(vc);
                } catch (CompletionException ex) {
                    // it means that doesn't exists the id in the collection
                    // why PGX ? :(
                }
            });


            for (PgxVertex<Integer> vertex : innerVerticesFiltered) {

                addNodesAndVertices(graph.getVertex(vertex.getId()).getOutEdges(), innerVertices, vertexCollection,
                        grouping, block, builder, true);

                addNodesAndVertices(graph.getVertex(vertex.getId()).getInEdges(), innerVertices, vertexCollection,
                        grouping, block, builder, false);

            }

        }

//        GraphChangeSet<Integer> changeSet = graph.createChangeSet();
//        innerVertices.forEach(v -> {
//            try {
//                if (graph.getVertex(v.getId()) != null)
//                    changeSet.updateVertex(v.getId()).setProperty(Constants.BLOCKING_PROPERTY, true);
//            } catch (CompletionException e) {
//                // it means that doesn't exists the id in the collection
//                // why PGX ? :(
//            }
//        });
//
//        return changeSet.build();
        filteredGraph = builder.build();
        return filteredGraph;
    }


    private void addNodesAndVertices(Collection<PgxEdge> edgesCollection, VertexCollection<Integer> innerVertices,
                                     VertexCollection<Integer> vertexCollection, int grouping, boolean block,
                                     GraphBuilder<Integer> builder, boolean out) {

        edgesCollection.stream()
                .filter(e -> {
                    try {
                        if (!out)
                            return vertexCollection.contains(e.getSource());
                        else
                            return vertexCollection.contains(e.getDestination());
                    } catch (CompletionException ex) {
                        // it means that doesn't exists the id in the collection
                        // why PGX ? :(
                        return false;
                    }
                })
                .forEach(e -> {

                    VertexBuilder<Integer> src = builder.addVertex((Integer) e.getSource().getId())
                            .setProperty(PrefixPropertyConstant.GROUPING.toString(), grouping);

                    VertexBuilder<Integer> dst = builder.addVertex((Integer) e.getDestination().getId())
                            .setProperty(PrefixPropertyConstant.GROUPING.toString(), grouping);

                    if (block) {
                        checkBlockingOfVertex(innerVertices, src, e, true);
                        checkBlockingOfVertex(innerVertices, dst, e, false);
                    }

                    builder.addEdge(e.getId(), src, dst)
                            .setLabel(e.getLabel());
                });
    }


    @Override
    public void checkBlockingOfVertex(VertexCollection<Integer> innerVertices, VertexBuilder<Integer> vb, PgxEdge edge,
                                      boolean isSrc) {
        try {

            if (isSrc && innerVertices.contains(edge.getSource()))
                vb.setProperty(Constants.BLOCKING_PROPERTY, true);
            if (!isSrc && innerVertices.contains(edge.getDestination()))
                vb.setProperty(Constants.BLOCKING_PROPERTY, true);

        } catch (CompletionException ex) {
            // it means that doesn't exists the id in the collection
            // why PGX ? :(
            // we ignore this case, we just don't block
        }
    }

    @Override
    public void close() throws Exception {
        if (filteredGraph != null)
            filteredGraph.destroy();
    }
}
