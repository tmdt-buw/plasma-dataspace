# PLASMA

## Platform for Auxiliary Semantic Modeling Approaches

PLASMA is a modular framework for semantic model creation. It features a GUI that allows users to create semantic models
without requiring deep knowledge about technologies and formalisms.
Additionally, PLASMA provides interfaces to attach additional (auxiliary) services to support the user during the modeling process.


## Architecture

PLASMA currently consists of the following services:

* **Semantic Modeling Service**: Service for semantic model creation, ontology storage and connecting auxiliary services
* **Data Processing Service**: Processes files and converts JSON data to RDF
* **Semantic Modeling UI**: A modeling UI with focus on simplifying the modeling process
* Spring Boot Admin Service: Monitoring
* Gateway Service: Offers a single entry point for all components

### Auxiliary Services

PLASMA currently offers the following auxiliary services:

* **ARS Label Matching**: Service for adding labels

### Storage

PLASMA is preconfigured to use MariaDB instances for the raw data management.
Semantic information (the Knowledge Graph) is stored using Apache Jena Fuseki.

### Software and Frameworks

PLASMA is written in Java, using Java 11 as the baseline version.
The included web-based frontend is written using Angular 13.

## Building PLASMA

### Requirements

* OpenJDK 11
* Docker
* Maven
* Node.js and npm

### Development Setup

* Build the containers with `mvn package jib:dockerBuild -DskipTests`
* Start the backend with `docker compose -f docker-compose.local.yml --profile staging up -d`
* Start the frontend with `npm run watch:smui` and `npm start` in `/frontend`
* Access the UI at [**localhost:80**](http://localhost:80)
