from SPARQLWrapper import SPARQLWrapper
from rdflib import Graph, RDF, Literal, XSD, Namespace


def get_triples_from_models(models) -> [(str, str, str)]:
    triples = []
    for model in models:
        triples += get_triples_from_model(model)
    return triples


def get_triples_from_model(model) -> [(str, str, str)]:
    triples = []
    if isinstance(model, Graph):
        for (s, p, o) in model.triples((None, None, None)):
            (s, p, o) = (str(s), str(p), str(o))
            triples.append((s, p, o))
    else:
        for triplet in model:
            triples.append((triplet[0],triplet[1],triplet[2]))

    # print(triples[:20])
    return triples


def collapse_literal_mappings(graph: Graph):
    """
    Replace all LiteralMapping instances in the graph with a single typed literal.
    Uses plcm:XSDDataType if present, defaults to xsd:string.
    Updates any triple pointing to the LiteralMapping to point to the literal.
    """
    PLCM = Namespace("http://plasma.uni-wuppertal.de/cm#")
    # Make a copy of the subjects to avoid modifying the graph while iterating
    for lm in list(graph.subjects(RDF.type, PLCM.LiteralMapping)):
        # Determine the literal value: use originalLabel if available
        label = next((str(o) for o in graph.objects(lm, PLCM.originalLabel)), "default")

        # Determine the datatype
        dtype = next((o for o in graph.objects(lm, PLCM.XSDDataType)), None)
        datatype = XSD.string if dtype is None else getattr(XSD, str(dtype).split("#")[-1], XSD.string)

        # Remove all triples describing the LiteralMapping
        for p, o in list(graph.predicate_objects(lm)):
            graph.remove((lm, p, o))

        # Replace any triples pointing to the LiteralMapping node
        for subj, pred in list(graph.subject_predicates(lm)):
            graph.remove((subj, pred, lm))
            graph.add((subj, pred, Literal(label, datatype=datatype)))

    print("LiteralMapping instances updated with typed literals.")
    return graph


def uninstanciate_graph(graph: Graph) -> Graph:
    type_mapping = {}
    for (sub, _, obj) in graph.triples((None, RDF.type, None)):
        type_mapping[sub] = obj

    uninstanciated_graph = Graph()
    for ns in graph.namespaces():
        uninstanciated_graph.namespace_manager.bind(ns[0], ns[1])

    collapse_literal_mappings(graph)


    for (sub, pred, obj) in graph.triples((None, None, None)):
        if pred == RDF.type:
            continue

        if sub in type_mapping:
            sub = type_mapping[sub]
        else:
            continue

        if obj in type_mapping:
            obj = type_mapping[obj]
        elif isinstance(obj, Literal):
            obj = obj.datatype
            if not obj:
                obj = XSD.string

        uninstanciated_graph.add((sub, pred, obj))

    return uninstanciated_graph