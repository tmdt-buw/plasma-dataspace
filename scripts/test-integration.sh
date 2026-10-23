#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
project="plasma-cleanup-tests-$$"
compose=(docker compose -p "$project" -f tests/integration.compose.yml)
trap '"${compose[@]}" down --volumes --remove-orphans' EXIT
"${compose[@]}" up -d --wait --wait-timeout 90
export PLASMA_TEST_DATABASE=true
export PLASMA_TEST_DATABASE_URL="jdbc:mariadb://127.0.0.1:${PLASMA_TEST_DATABASE_PORT:-13316}/plasma_cleanup_test"
export PLASMA_TEST_FUSEKI="http://127.0.0.1:${PLASMA_TEST_FUSEKI_PORT:-18303}/cleanup-models"
mvn --batch-mode --no-transfer-progress -pl components/semantic-modeling-service/service,components/data-processing-service/service -am verify
export PLASMA_TEST_TRIPLESTORE=true
export SPARQL_SM_STORE="$PLASMA_TEST_FUSEKI"
export SPARQL_ONTOLOGIES_STORE="http://127.0.0.1:${PLASMA_TEST_FUSEKI_PORT:-18303}/cleanup-ontologies"
(cd auxiliary_services/ARS-R-statistic-recommender && python3 -m pytest test)
