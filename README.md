## Query driven summarization

# Getting Started

1. Clone the project
2. Execute the file `dependencies.sh`. But first modify your PGX_HOME.
3. Execute `mvn assembly:assembly -DdescriptorId=jar-with-dependencies`.
4. Execute

```bash
$ java -cp Label-driven-1.0.0-jar-with-dependencies.jar -Xmx2048M -Dpgx.max_off_heap_size=1024000 -XX:-UseGCOverheadLimit -Dlog4j.debug -Dlog4j.configurationFile=~/label-driven-summarization/src/main/resources/log4j.properties label.driven.summarization.Main "~/graphcon/query-driven-summarization/src/main/resources/summaries/1000_social_network_none_attribut" 
```


# Gmark

1. Generate graph in gMark

```bash
$ ./test -n 1000 -a -c ../use-cases/social-network.xml -g ../demo/social/social-1000 -w ../demo/social/social-workload-1000.xml -r ../demo/social/

$ ./test -n 1000 -a -c ../use-cases/test.xml -g ../demo/test/test-1000 -w ../demo/test/test-workload-1000.xml -r ../demo/test/

```