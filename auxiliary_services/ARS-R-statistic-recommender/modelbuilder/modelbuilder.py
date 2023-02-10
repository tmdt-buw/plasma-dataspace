import json
import pickle
import config
from util.parse import get_classes_and_predicates, generate_id_dict, encode_triples


def get_models():
    models = [
        [
            [
                "http://plasma.uni-wuppertal.de/schema#latitude",
                "http://plasma.uni-wuppertal.de/schema#definedBy",
                "http://plasma.uni-wuppertal.de/schema#wgs84"
            ],
            [
                "http://plasma.uni-wuppertal.de/schema#geoposition",
                "http://plasma.uni-wuppertal.de/schema#consistsOf",
                "http://plasma.uni-wuppertal.de/schema#longitude"
            ],
            [
                "http://plasma.uni-wuppertal.de/schema#longitude",
                "http://plasma.uni-wuppertal.de/schema#definedBy",
                "http://plasma.uni-wuppertal.de/schema#wgs84"
            ],
            [
                "http://plasma.uni-wuppertal.de/schema#location",
                "http://plasma.uni-wuppertal.de/schema#has",
                "http://plasma.uni-wuppertal.de/schema#address"
            ],
            [
                "http://plasma.uni-wuppertal.de/schema#location",
                "http://plasma.uni-wuppertal.de/schema#definedBy",
                "http://plasma.uni-wuppertal.de/schema#geoposition"
            ],
            [
                "http://plasma.uni-wuppertal.de/schema#geoposition",
                "http://plasma.uni-wuppertal.de/schema#consistsOf",
                "http://plasma.uni-wuppertal.de/schema#latitude"
            ],
            [
                "http://plasma.uni-wuppertal.de/schema#location",
                "http://plasma.uni-wuppertal.de/schema#has",
                "http://plasma.uni-wuppertal.de/schema#geo_type"
            ]
        ]
    ]
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