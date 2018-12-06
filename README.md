## Query driven summarization

# Getting Started

1. Clone the project
2. Download [PGX 3.1](https://www.oracle.com/technetwork/oracle-labs/parallel-graph-analytix/downloads/index.html) (not available yet).
3. Execute the file [`dependencies-3.1.0.sh`](label-driven-summarization/scripts/dependencies-3.1.0.sh). But first modify your PGX_HOME and the three variables in the file to point to PGX libraries.
4. Execute `mvn assembly:assembly -DdescriptorId=jar-with-dependencies`.
5. Execute

```bash
$ java -cp Label-driven-1.0.0-jar-with-dependencies.jar -Dpgx.max_off_heap_size=<HEAD_SIZE> -Dlog4j.configurationFile=<LOG_PROPERTIES_PATH> label.driven.summarization.Main <FOLDER_WITH_FILES>
```


# How to generate graphs

You can generate an artificial graph using [gMark](https://github.com/graphMark/gmark). Find the instructions in their own repository. Here some examples:

```bash
$ ./test -n 1000 -a -c ../use-cases/social-network.xml -g ../demo/social/social-1000 -w ../demo/social/social-workload-1000.xml -r ../demo/social/

$ ./test -n 1000 -a -c ../use-cases/test.xml -g ../demo/test/test-1000 -w ../demo/test/test-workload-1000.xml -r ../demo/test/

```
