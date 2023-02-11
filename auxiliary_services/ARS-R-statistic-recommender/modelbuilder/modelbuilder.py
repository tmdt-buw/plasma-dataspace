import json
import os
import pickle

from rdflib import RDF, RDFS, OWL, URIRef, Graph, Literal, XSD

import config
from util.namespaces import PREFIXES
from util.parse import get_classes_and_predicates, generate_id_dict, encode_triples
from SPARQLWrapper import SPARQLWrapper, JSON, TURTLE, XML
from util.graphutil import uninstanciate_graph, get_triples_from_model


def get_models():
    # establish connection to triple store (currently Fuseki)
    host = os.getenv('sparql_endpoint')

    sparql = SPARQLWrapper(host)
    sparql.setQuery(PREFIXES + """
        SELECT ?id WHERE {
          ?model <http://plasma.uni-wuppertal.de/cm#cmUuid> ?id .
        } LIMIT 100
    """)
    # sparql.setCredentials("admin", "plasma")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()

    print("found models", len(results["results"]["bindings"]))

    models = []

    for result in results["results"]["bindings"]:
        model_id = result["id"]['value']
        print('processing', model_id)

        query = PREFIXES + f"""
                    DESCRIBE ?node WHERE {{
                      ?model <http://plasma.uni-wuppertal.de/cm#cmUuid> "{model_id}" .
                      ?model <http://plasma.uni-wuppertal.de/cm#hasNode> ?node .
                      ?node ?p ?o .
                    }}
                """

        sparql.setReturnFormat(XML)
        sparql.setQuery(query)

        model_graph:Graph = sparql.queryAndConvert()

        triples = list(model_graph.triples((None, None, None)))
        print('model size', len(triples))

        model_graph = uninstanciate_graph(model_graph)

        triples = list(model_graph.triples((None, None, None)))
        print('model size', len(triples))

        triples = get_triples_from_model(model_graph)
        models.append(triples)

    with open(config.CACHE.RAW_MODELS_PATH, "w") as file:
        json.dump(models, file)


def generate_mappings():
    with open(config.CACHE.RAW_MODELS_PATH, "r") as file:
        models = json.load(file)

    triples = []
    for model in models[:]:
        for triple in model:
            triples.append(tuple(triple))

    classes, predicates = get_classes_and_predicates(triples)
    classes_mapping = generate_id_dict(classes)
    predicates_mapping = generate_id_dict(predicates)
    with open(config.CACHE.CLASSES_MAPPING_PATH, "w") as file:
        json.dump(classes_mapping, file)

    with open(config.CACHE.PREDICATES_MAPPING_PATH, "w") as file:
        json.dump(predicates_mapping, file)

    encoded_triples = encode_triples(triples, classes_mapping, predicates_mapping)
    with open(config.CACHE.ENCODED_TRIPLES_PATH, "w") as file:
        json.dump(pickle.dumps(encoded_triples).decode('latin-1'), file)

    return classes_mapping, predicates_mapping
