# Data Processing Service (DPS)

PLASMA's service for receiving sample data, conversion and data-processing operations. RDF conversion uses the supplied semantic model; supported converter modules cover structured formats such as CSV, JSON, GeoJSON and ESRI. The integration tests include nested arrays, literal field names and preservation of stored data.

See the [integration README](../../README.md) for full startup and storage boundaries. The standalone service port is 8182; the Dataspace exposes its API through `/plasma/api/dps`. Its local storage directory and persisted volume are configured by the deployment. It does not write arbitrary converted resources into a Solid Pod automatically.

From the repository root with JDK 21 and Maven:

```sh
mvn -pl components/data-processing-service/service -am test
```

Use the full disposable integration suite from the root README for database/storage checks. The Angular client is generated from the service's OpenAPI description; see [frontend/README.md](../../frontend/README.md).
