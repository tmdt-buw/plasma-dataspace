# Semantic Modeling Service (SMS)

The PLASMA service for ongoing modelings, semantic modifications, ontology management and finalized semantic models. It uses MariaDB for modeling state and Fuseki for semantic data. See the [integration README](../../README.md) for complete startup, gateway routing and upstream provenance.

## Development and tests

Use JDK 21 and Maven from the repository root so shared reactor modules are available:

```sh
mvn -pl components/semantic-modeling-service/service -am test
```

Database/Fuseki integration tests additionally require the disposable services described in the root README. The configured standalone service port is 8181; use the proxy for browser access. Do not expose the integrated service directly around the Dataspace access gateway.

## Configuration

Settings live in `service/src/main/resources` and are overridden by the selected deployment. Relevant local-ontology properties include:

```properties
plasma.kgs.localontology.prefix=local
plasma.kgs.localontology.uri=https://local.ontology#
plasma.kgs.localontology.label=LOCAL
plasma.kgs.localontology.description=A local ontology maintained by this instance.
```

`plasma.sms.feature.syntacticoperations.enabled` enables syntactic model edits; consumers must support changed syntax before enabling this. `plasma.sms.feature.recommendations.enabled` enables the optional recommendation path. Auxiliary recommendation services are not required for manual modeling and are disabled in the lightweight Dataspace profile. No former standalone SRS service needs to be added to that profile.
