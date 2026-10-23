import json
import os
import pickle
from typing import Dict

from SPARQLWrapper import SPARQLWrapper, JSON
from rdflib import Graph

import config
from util.graphutil import uninstanciate_graph
from util.namespaces import PREFIXES, PLCM
from util.parse import get_classes_and_predicates, generate_id_dict, encode_triples


def get_models():
    # establish connection to triple store (currently Fuseki)
    host = os.getenv('SPARQL_SM_STORE')
    if not host:
        print("WARN", "No SPARQL endpoint defined to request semantic models. 'SPARQL_SM_STORE' is empty.")
        return

    sparql = SPARQLWrapper(host)
    sparql.setQuery(PREFIXES + """
        SELECT DISTINCT ?model WHERE {
          ?model a plcm:SemanticModel ; plcm:cmUuid ?id .
        } LIMIT 100
    """)
    # sparql.setCredentials("admin", "plasma")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()

    print("found models", len(results["results"]["bindings"]))

    models = []

    for result in results["results"]["bindings"]:
        model_id = result["model"]['value']
        print('processing', model_id)
        query = PREFIXES + f"""
            CONSTRUCT {{ ?node ?p ?o }}
                WHERE {{
                ?node ?p ?o .
                FILTER(STRSTARTS(STR(?node), '{model_id}'))
                }}
        """

        #print("executed query", query)
        sparql = SPARQLWrapper(host)
        sparql.setQuery(query)
        #sparql.setReturnFormat(TURTLE)

        model_graph: Graph = sparql.query().convert()
        print('model size after pull', len(model_graph))

        model_graph = uninstanciate_graph(model_graph)
        print('uninstanciated model size', len(model_graph))

        triples = list(
            [triple for triple in model_graph.triples((None, None, None)) if not str(triple[1]).startswith(PLCM)])
        if len(triples) == 0:
            continue
        print('model size post-clean', len(triples))

        # triples = get_triples_from_model(model_graph)
        # triples = list(
        #     [triple for triple in triples if not str(triple[1]).startswith(PLCM)])
        models.append(triples)

    with open(config.CACHE.RAW_MODELS_PATH, "w") as file:
        json.dump(models, file)


def build_label_index():
    host = os.getenv('SPARQL_ONTOLOGIES_STORE')
    if not host:
        print("WARN", "No SPARQL endpoint defined to request ontologies. 'SPARQL_ONTOLOGIES_STORE' is empty.")
        return

    sparql = SPARQLWrapper(host)
    sparql.setQuery(PREFIXES + """
            SELECT ?uri ?label ?comment WHERE {
                    ?uri rdfs:label ?label .
                    OPTIONAL { ?uri rdfs:comment ?comment }
        } 
        """)
    # sparql.setCredentials("admin", "plasma")
    sparql.setReturnFormat(JSON)
    results = sparql.query().convert()

    meta_info: Dict[str, {str, str}] = {}

    for result in results["results"]["bindings"]:
        uri = result["uri"]['value']
        label = result["label"]['value']
        comment = result.get("comment", {}).get("value")

        meta_info[uri] = {'label':label, 'comment':comment}

    return meta_info


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
