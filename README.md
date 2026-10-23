# PLASMA
## Platform for Auxiliary Semantic Modeling Approaches

PLASMA is a modular framework for semantic model creation. It features a GUI that allows users to create semantic models
without requiring deep knowledge about technologies and formalisms.
Additionally, PLASMA provides interfaces to attach additional (auxiliary) services to support the user during the modeling process.


## Architecture
PLASMA currently consists of the following (micro)services: 

* **Semantic Modeling Service**: Service for semantic model creation 
* **Data Processing Service**: Processes files and converts JSON data to RDF
* Semantic Modeling UI: A modeling UI with focus on simplifying the modeling process

### Auxiliary Services

PLASMA currently offers the following auxiliary services:

* **ARS Label Matching**: Service for adding labels (currently set to NOOP)
* **ARS Statistical Recommender**: Recommends extensions to semantic models based on existing semantic models

These services are designed and configured for demonstration purposes and can serve as a blueprint for own extensions.

### Storage
PLASMA is preconfigured to use MariaDB instances for the raw data management.
Semantic information (the Knowledge Graph) is stored using Apache Jena Fuseki.

### Software and Frameworks
PLASMA is written in Java, using Java 21 as the baseline version.
The included web-based frontend is written using Angular 13.

## Building PLASMA

### Requirements
* Install OpenJDK 21 or newer
* Install Docker with Docker Compose
* Install Maven to build the Java services
* The frontend uses npm internally. Check the dedicated frontend README for development information.

### Docker Setup

Run the following commands from the repository root. Docker must already be running.

```shell
mvn -U -pl components/semantic-modeling-service/service,components/data-processing-service/service -am package jib:dockerBuild -DskipTests
docker compose -f docker-compose.local.yml up -d --build fuseki mariadb-sms traefik sms dps frontend
```

This starts the core stack needed for manual modeling. The optional auxiliary recommendation service is not required.

### Docker Hub images for Solid Dataspace

The GitHub workflow `.github/workflows/docker-images.yml` compiles and packages the Java reactor and publishes only the three
images required by the Solid Dataspace integration:

- `son1i/plasma-sms`
- `son1i/plasma-dps`
- `son1i/plasma-frontend`

Configure the GitHub repository secrets `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN`; that account/token must have push
access to the three `son1i/*` repositories. A push to `main` or the repository's existing `master` branch creates a
matching branch tag and an immutable `sha-*` tag; a tag such as `release-1.8.2-test.1` creates `1.8.2-test.1` and
`sha-*`. The frontend image is built with base href `/plasma/` and sends its API requests through `/plasma/api/sms`
and `/plasma/api/dps`. MariaDB and Fuseki remain pinned third-party images and are not built by this workflow.

### Tests

Run the Java suites with JDK 21 from the repository root:

```shell
mvn -pl components/semantic-modeling-service/service,components/data-processing-service/service -am test
```

The core suites cover RDF conversion (including nested arrays and literal field names),
CSV/GeoJSON/ESRI conversion, storage preservation, semantic model metadata round trips,
and modeling history. The browser suites exercise the conversion dialog and modeling
URI editor using real templates in headless Chrome:

```shell
cd frontend
npm ci
npm test
```

Set `CHROME_BIN` when Chrome/Chromium is not on the default path.
For the complete integration suite, install the recommendation test dependencies in
an isolated Python 3.11 environment, then run:

```shell
python3 -m pip install -r auxiliary_services/ARS-R-statistic-recommender/requirements-test.txt
./scripts/test-integration.sh
```

The script creates a separate Compose project with disposable MariaDB and Fuseki
services, runs Java verification plus the recommendation API/store tests, then removes
only that temporary project. Ports 13316 and 18303 are bound to loopback; override
`PLASMA_TEST_DATABASE_PORT` / `PLASMA_TEST_FUSEKI_PORT` when needed. It never mounts
existing application data. The publishing workflow runs these checks before publishing.

When running tests manually against equivalent disposable services, set
`PLASMA_TEST_DATABASE=true`, `PLASMA_TEST_DATABASE_URL`, `PLASMA_TEST_FUSEKI`,
`PLASMA_TEST_TRIPLESTORE=true`, `SPARQL_SM_STORE`, and `SPARQL_ONTOLOGIES_STORE`.
Database tests use the `plasma_cleanup_test` database and require a disposable URL;
RDF integration tests require explicitly named `cleanup` datasets. Without these
settings only the integration cases are skipped; API and core unit tests remain active.


### Starting PLASMA

Open [http://localhost/](http://localhost/) after the containers have started. Use this proxied URL rather than the
frontend's direct port so that both `/api/sms` and `/api/dps` are routed correctly.

To create a semantic model:

1. Open **Modelings** and choose **Create Modeling**.
2. Add one or more JSON samples and run the schema analysis.
3. Create and open the modeling.
4. Select an uploaded ontology, or create provisional classes and relations in the editor.
5. Map the syntactic nodes, save the model, and finalize it. Finalized models can be exported as Turtle.

On a fresh instance, PLASMA creates the writable `LOCAL` ontology automatically. Additional Turtle ontologies can be
uploaded in the ontology management view.

### Stopping PLASMA

```shell
docker compose -f docker-compose.local.yml down
```

## Development Setup

For development, infrastructure services can be started exclusively using `docker compose up -d`.

### Developing Services and Frontend

Each service has to be started manually in dev mode.

* Start each service using either Spring Boot, FastAPI or Maven
* Start the frontend with `npm run watch:smui` and `npm start` in `/frontend`
* Access the UI at [**localhost:4200**](http://localhost:4200)

### Development Config for IDEs

By default, the configuration parameters are set for local development mode (run via IDE).
When utilizing a Docker deployment, these values are overwritten by the compose configuration.
IDEs offers the option to overwrite configuration files for specific applications.

#### SMS

| Name                                    | Value                                     |
|-----------------------------------------|-------------------------------------------|
| `plasma.config.local.path`              | ` C:\temp`                                |
| `plasma.maria.url`                      | ` jdbc:mysql://localhost:3306/plasma-sms` |

#### ARS-L-LM

| Name                                | Value                                           |
|-------------------------------------|-------------------------------------------------|
| `plasma.registration.upstream.host` | `localhost`                                     |
| `plasma.registration.upstream.port` | `8181`                                          |
| `plasma.registration.interval`      | `30000`                                         |
| `plasma.registration.path`          | `/api/sms/ars/registration'                     |
| `plasma.kgs.ontology.api`           | `http://localhost:8888/api/plasma-kgs/ontology` |

#### ARS-R-SR

| Name                                  | Value                                              |
|---------------------------------------|----------------------------------------------------|
| `PLASMA_REGISTRATION_URL`             | ` http://localhost:8181/api/sms/ars/registration`  |
| `PLASMA_ARS_API_URL`                  | ` http://localhost:8221/recommendation`            |
| `SERVICE_PORT`                        | ` 8191`                                            |
| `SPARQL_ONTOLOGIES_STORE`             | ` http://localhost:3030/ontologies`                |
| `SPARQL_SM_STORE`                     | ` http://localhost:3030/sm`                        |

### Default port mappings

These port mappings are only relevant when accessing the services via localhost. Inside the Docker network, all services
have their own host. Adjust the Docker port mappings if a deviation is needed (e.g. for testing). This list should give
an overview of the services and ports occupied and is for information purposes only.

| Name     | Service |
|:---------|--------:|
| Database |    3306 |
| SMS      |    8181 |
| DPS      |    8182 |
| Frontend | 80 (proxy), 81 (direct) |
| ARS-L-LM |    8190 |
| ARS-R-SR |    8221 |
