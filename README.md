# JME Architecture Repository Example

This example shows how to set up and run the jEAP Architecture Repository (archrepo) service locally.
It contains the following modules:

* **jme-archrepo-service**: An instance of the Architecture Repository service, powered by
  [jeap-archrepo-web](https://github.com/jeap-admin-ch/jeap-archrepo-service). It stores and displays
  architecture documentation including database schemas, OpenAPI specifications, and system relationships.
* **jme-archrepo-auth-scs**: An instance of the OAuth mock server to authenticate against the Architecture Repository.

## Changes

This project is versioned using [Semantic Versioning](http://semver.org/) and all changes are documented in
[CHANGELOG.md](./CHANGELOG.md) following the format defined in [Keep a Changelog](http://keepachangelog.com/).

## Prerequisites

To use this project, ensure you have the following installed:

1. **Java Development Kit (JDK)**: Version 25.
2. **Docker**: For running the required infrastructure (PostgreSQL).

**Note:** Use the provided Maven wrapper to build and run the project.

## Getting started

### Infrastructure

Before the application can be started, the infrastructure (PostgreSQL) must be running:

```shell
docker-compose -f docker/docker-compose.yml up
```

### Build

The project can be built with:

```shell
./mvnw install
```

### Start

Then the individual modules can be started using:

```shell
./mvnw --projects jme-archrepo-auth-scs spring-boot:run -Dspring-boot.run.profiles=local
./mvnw --projects jme-archrepo-service spring-boot:run -Dspring-boot.run.profiles=local
```

### Access

Once started, the Swagger UI is available at:

[http://localhost:8080/jme-archrepo-service/swagger-ui/index.html](http://localhost:8080/jme-archrepo-service/swagger-ui/index.html)

## Profiles

* **application-local**: Contains all configurations for running the application locally (local PostgreSQL, mock OAuth server).

## Integration Tests

The `jme-archrepo-test` module contains end-to-end integration tests that verify that the Architecture Repository
service and its configuration work: it covers Docker Compose infrastructure and both service modules.

### How it works

The test uses Spring Boot Docker Compose support to automatically start and stop the Docker infrastructure
(PostgreSQL) before and after the test run. It then starts the two Spring Boot services (auth-scs and
archrepo-service) as Maven subprocesses via `mvnw spring-boot:run` and polls their health endpoints until they
are ready.

The tests themselves use REST-Assured to interact with the services:

- **`registerSystemAndVerifyModel`** — Registers a system via the management API and verifies it appears in the model.
- **`modelEndpointsShouldReturnSuccessfully`** — Verifies that the REST-API-relation and system-components endpoints respond correctly.
- **`shouldObtainAccessTokenFromAuthScs`** — Verifies the OAuth flow works end-to-end with the mock server.

### Running locally

```shell
# Build and install all local modules
./mvnw install -pl '!:jme-archrepo-test'
# Run integration tests
./mvnw verify -pl jme-archrepo-test
```

This will:

1. Start the Docker Compose infrastructure (PostgreSQL container is stopped after the test).
2. Build and start the two Spring Boot services on ports 8080 and 8081.
3. Run the integration tests.
4. Stop all services and containers.

Ensure Docker is running and ports 5432, 8080, and 8081 are available.

### Running on CI

On CI the `CI` environment variable must be set. This activates the `ci` Spring profile which uses
`docker-compose-ci.yml` as an overlay (removing host port bindings and using the container hostname for the
PostgreSQL datasource URL). On CI, an isolated Docker network is used to allow for parallel builds.

## Note

This repository is part of the open source distribution of JME. See [github.com/jme-admin-ch/jme](https://github.com/jme-admin-ch/jme)
for more information.

## License

This repository is Open Source Software licensed under the [Apache License 2.0](./LICENSE).
