# Label-matching auxiliary service

An optional PLASMA labeling-service example. The upstream design matches syntax-node labels against ontology class labels. The current platform overview identifies its label-matching integration as NOOP; it is not part of the lightweight Solid Dataspace core profile and should not be advertised as an active automatic labeler there.

The module remains in the Java reactor as a reference for auxiliary services. Use the [root README](../../README.md) for supported services and configuration. Before enabling an auxiliary integration, check its implemented behavior, registration URL and ontology access against the chosen SMS deployment.

From the repository root:

```sh
mvn -pl auxiliary_services/ARS-L-label-matching -am test
```
