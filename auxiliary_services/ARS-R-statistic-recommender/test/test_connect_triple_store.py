import os
import unittest
import uuid
from pathlib import Path
from typing import List
from unittest import mock

from SPARQLWrapper import SPARQLWrapper, JSON, TURTLE, POST, INSERT
from rdflib import URIRef, Graph, Namespace, Literal, RDF

from modelbuilder import modelbuilder
import service
from modelbuilder.modelbuilder import PREFIXES
from schemas import CombinedModel, SemanticModel, SemanticModelNode
from util.namespaces import PLCM, PLSM

sparql_query_host = "http://localhost:3030/kgs/"
sparql_update_host = "http://localhost:3030/kgs/"

clear_database_query = """
    DELETE {
    ?s ?p ?o .
} WHERE {
    ?s ?p ?o .
}
"""




def get_semantic_models() -> Graph:

    kg: Graph = Graph()
    count = 1
    for file in os.listdir("testdata"):
        g: Graph = Graph()
        g.parse(Path("testdata", file), format="ttl")
        model_node: URIRef = URIRef("model-" + str(count), PLSM)

        for s in g.subjects(None, None):
            g.add((model_node, PLCM.hasNode, s))

        g.add((model_node, PLCM.cmUuid, Literal(str(uuid.uuid4()))))
        g.add((model_node, RDF.type, PLCM.SemanticModel))
        for namespace in g.namespace_manager.namespaces():
            kg.namespace_manager.bind(namespace[0], namespace[1])

        kg.__iadd__(g)
        count += 1
    print(len(kg))
    return kg


def generate_insert_data_query(triples: List):
    """
    Generate an update query to add the given triples to a triple store
    """
    q_header = """
        INSERT DATA {
           """
    triples_data = ''.join([f'{s.n3()} {p.n3()} {o.n3()} .\n' for (s, p, o) in triples])

    q_tail = """
        }
        """
    return q_header + triples_data + q_tail


class TestStringMethods(unittest.TestCase):


    def prepare_triple_store(self):
        print(PREFIXES)
        sparql = SPARQLWrapper(sparql_update_host)
        sparql.setQuery(modelbuilder.PREFIXES + """
                SELECT (count(?s) as ?count) WHERE {
                  ?s ?p ?o .
                }
            """)
        # sparql.setCredentials("admin", "plasma")
        sparql.setReturnFormat(JSON)
        results = sparql.query().convert()

        count = int(results["results"]["bindings"][0]["count"]['value'])

        if count > 0:
            return

        kg = get_semantic_models()

        query = generate_insert_data_query(list(kg.triples((None,None,None))))
        sparql.setMethod(POST)
        sparql.setQuery(query)
        sparql.query()

    @mock.patch.dict(os.environ, {'sparql_endpoint': sparql_query_host})
    def test_connect_triple_store(self):
        self.prepare_triple_store()
        modelbuilder.get_models()

        # cm: CombinedModel = CombinedModel()
        # cm.recommendations = []
        # cm.semanticModel = SemanticModel()
        # smn = SemanticModelNode.parse_obj({"_class": "Class"})
        # smn.uri = "http://plasma.uni-wuppertal.de/schema#latitude"
        # cm.semanticModel.nodes = [smn]
        # cm_with_recommendations: CombinedModel = service.generate_recommendations(combinedModel=cm)
        # print("recommendations", cm_with_recommendations.recommendations)


if __name__ == '__main__':
    unittest.main()
