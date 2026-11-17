# PLASMA data model

Shared Java representations of syntactic, semantic and combined models, including model transformations and generators. This is a library used by the services, not a separately running application.

From the repository root with JDK 21 and Maven:

```sh
mvn -pl components/datamodel -am test
```

Model changes can affect persistence, RDF export and frontend/API compatibility. Verify the dependent SMS/DPS suites through the [root integration workflow](../../README.md) before publishing a changed model contract. Preserve existing model identifiers and stored representations unless a separately reviewed migration is provided.
