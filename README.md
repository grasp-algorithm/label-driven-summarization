## Query driven summarization

GRASP is a query-driven summarization algorithm targeting property graphs. It allows for the approximate estimation of counting graph queries on property graphs.

# Getting Started

1. Clone the project or download the project.
2. It requires the latest version of [PGX 3.1](https://www.oracle.com/technetwork/oracle-labs/parallel-graph-analytix/downloads/index.html) (courtesy of Oracle Labs).
3. Execute the file [`dependencies-3.1.0.sh`](label-driven-summarization/scripts/dependencies-3.1.0.sh). But first modify your PGX_HOME and the three variables in the file to point to PGX libraries.
4. Execute `mvn clean compile package`.
5. Execute the following command:

```bash
$ OPTS="-Xms<MIN_MEMORY_USE>g -Xmx<MAX_MEMORY_USE>g \
-Dpgx.max_off_heap_size=<MAX_MEMORY_USE>000000 \
-XX:-UseGCOverheadLimit \
-Dlog4j.debug \
-Dlog4j.configurationFile=<LOG4J_PROPERTIES>"

$ java OPTS -jar Label-driven-1.0.0-jar-with-dependencies.jar <FOLDER_WITH_FILES>
```
Where:
* `<MIN_MEMORY_USE>` : The minimal use of memory allowed.
* `<MAX_MEMORY_USE>` : The maximal use of memory allowed.
* `<LOG4J_PROPERTIES>` : a log4j file properties to specify the level of log and the path to the directory.
* `<FOLDER_WITH_FILES>` is the path of a directory with required files:
    - edge_file.txt: A required file with triplets or edges of the graph (gMark output format).
    - vertex_file.txt: An optional file with the id of all the vertices and potentially also with labels.
    - schema.json: A required file with the description of what algorithms will be applied to the input graph.

# How to generate graphs

To use GRASP you need a graph, with the output format of gMark.
You can generate an artificial graph using [gMark](https://github.com/graphMark/gmark). Find the instructions in their own repository. Here some examples:

```bash
$ ./test -n 1000 -a -c ../use-cases/social-network.xml -g ../demo/social/social-1000 -w ../demo/social/social-workload-1000.xml -r ../demo/social/

$ ./test -n 1000 -a -c ../use-cases/test.xml -g ../demo/test/test-1000 -w ../demo/test/test-workload-1000.xml -r ../demo/test/
```

# Configuration
An example configuration file as an example can be found [here](https://github.com/grasp-algorithm/label-driven-summarization/blob/master/src/main/resources/summaries/running-example/schema.json).
The important attributes are:

* *mergingProcedure*: Where should be included the selected heuristic (TARGET_MERGE or SOURCE_MERGE).
* *computeConcatenationProperties*: To specify if the graph will be submitted to concatenation queries which require more properties.
* *allLabelsAllowed*: Which is taken into account only if the precedent property is `true`, to specify if all the labels on the
edges will be submitted to concatenation queries. If only some of them will be used in the queries, you can use the property *allowedLabels* to specify pairs of labels, where an entry of the array could be `follows:author`.


# Example
An example is provided in the [resources folder](https://github.com/grasp-algorithm/label-driven-summarization/tree/master/src/main/resources/summaries/running-example).
There you can find the required files (same example as the paper) and the command.

```bash
$ OPTS="-Xms1g -Xmx1g \
-Dpgx.max_off_heap_size=1000000 \
-XX:-UseGCOverheadLimit \
-Dlog4j.debug"

$ java OPTS -jar Label-driven-1.0.0-jar-with-dependencies.jar /resources/summaries/running-example/
```

Once the command finish its execution, the last line will be:

|labels| verticesOG | edgesOG | verticesSG | edgesSG | runtime | properties | crVertices | crEdges   |
| ---- | ---------- | ------- | ---------- | ------- | ------- | ---------- | ---------- | --------- | 
|    7 |         25 | 37      |       3    |      4  |    1320 |         10 |       0.88 | 0.8918919 |

Where:
* **labels**: represent the number of labels in the original graph.
* **verticesOG**: the number of vertices in the original graph.
* **edgesOG**: the number of edges in the original graph.
* **verticesSG**: the number of vertices in the summary.
* **edgesSG**: the number of vertices in the summary.
* **runtime**: the runtime in microseconds.
* **properties**: the number of computed properties.
* **crVertices**: the compression ratio on the vertices.
* **crEdges**: the compression ratio on the edges.

** _Since the available version of PGX works on homogeneous graphs, rather than on heterogeneous ones, we padded each node in the graph summary with the same properties as in the other nodes_

Once the summary is build, you can pass to the [next project](https://github.com/grasp-algorithm/approximative-query-processing) to execute the queries.
