## Query driven summarization

# Getting Started

1. Clone the project
2. Download [PGX 3.1](https://www.oracle.com/technetwork/oracle-labs/parallel-graph-analytix/downloads/index.html) (not available yet).
3. Execute the file [`dependencies-3.1.0.sh`](https://github.com/grasp-algorithm/label-driven-summarization/blob/grasp-algorithm-readme/scripts/dependencies-3.1.0.sh). But first modify your PGX_HOME and the three variables in the file to point to PGX libraries.
4. Execute `mvn assembly:assembly -DdescriptorId=jar-with-dependencies`.
5. Execute

```bash
$ java -cp Label-driven-1.0.0-jar-with-dependencies.jar -Xmx2048M -Dpgx.max_off_heap_size=1024000 -XX:-UseGCOverheadLimit -Dlog4j.debug -Dlog4j.configurationFile=~/label-driven-summarization/src/main/resources/log4j.properties label.driven.summarization.Main "~/graphcon/query-driven-summarization/src/main/resources/summaries/1000_social_network_none_attribut" 
```


# Gmark

1. Generate graph in gMark

```bash
$ ./test -n 1000 -a -c ../use-cases/social-network.xml -g ../demo/social/social-1000 -w ../demo/social/social-workload-1000.xml -r ../demo/social/

$ ./test -n 1000 -a -c ../use-cases/test.xml -g ../demo/test/test-1000 -w ../demo/test/test-workload-1000.xml -r ../demo/test/

```
