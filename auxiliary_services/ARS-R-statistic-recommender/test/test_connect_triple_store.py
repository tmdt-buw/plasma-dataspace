"""Integration against explicitly supplied, disposable Fuseki datasets."""
import json
import os
import uuid
from unittest.mock import patch
import pytest
import requests
from rdflib import Graph, Literal, Namespace, RDF, RDFS, URIRef
from modelbuilder import modelbuilder
from util.namespaces import PLCM

pytestmark = pytest.mark.skipif(os.getenv("PLASMA_TEST_TRIPLESTORE") != "true", reason="Needs disposable Fuseki datasets")


def update(endpoint, query):
    assert "cleanup" in endpoint, "Only disposable cleanup datasets are allowed"
    response = requests.post(endpoint, data={"update": query}, timeout=15)
    response.raise_for_status()


def triples(graph):
    return "\n".join(f"{s.n3()} {p.n3()} {o.n3()} ." for s, p, o in graph)


def test_reads_models_and_optional_ontology_descriptions(tmp_path, monkeypatch):
    models = os.environ["SPARQL_SM_STORE"]
    ontologies = os.environ["SPARQL_ONTOLOGIES_STORE"]
    prefix = Namespace(f"urn:cleanup:{uuid.uuid4()}:")
    graph = Graph()
    for index in range(2):
        base = URIRef(prefix[f"model-{index}"])
        node = URIRef(str(base) + "-node")
        graph.add((base, RDF.type, PLCM.SemanticModel))
        graph.add((base, PLCM.cmUuid, Literal(str(uuid.uuid4()))))
        graph.add((node, RDF.type, prefix.Station))
        graph.add((node, PLCM.cmUuid, Literal(str(uuid.uuid4()))))
        graph.add((node, prefix.measures, Literal(index)))
    ontology = Graph()
    ontology.add((prefix.Station, RDFS.label, Literal("Station")))
    ontology.add((prefix.Station, RDFS.comment, Literal("Example station")))
    ontology.add((prefix.measures, RDFS.label, Literal("measures")))
    cache = tmp_path / "models.json"
    monkeypatch.setattr(modelbuilder.config.CACHE, "RAW_MODELS_PATH", str(cache))
    try:
        update(models, "INSERT DATA {" + triples(graph) + "}")
        update(ontologies, "INSERT DATA {" + triples(ontology) + "}")
        modelbuilder.get_models()
        stored = json.loads(cache.read_text())
        own = [m for m in stored if any(t[0] == str(prefix.Station) for t in m)]
        assert len(own) == 2  # Node UUID metadata must not be mistaken for another model.
        assert all(len(m) == 1 and m[0][1] == str(prefix.measures) for m in own)
        metadata = modelbuilder.build_label_index()
        assert metadata[str(prefix.Station)] == {"label": "Station", "comment": "Example station"}
        assert metadata[str(prefix.measures)] == {"label": "measures", "comment": None}
    finally:
        update(models, "DELETE DATA {" + triples(graph) + "}")
        update(ontologies, "DELETE DATA {" + triples(ontology) + "}")
