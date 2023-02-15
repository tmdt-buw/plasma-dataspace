import os
import uuid
from typing import List, Dict
from uuid import UUID

import uvicorn
from fastapi import FastAPI
from pydantic import BaseModel

import modelbuilder.modelbuilder
from linkedmodelextension import linked_model_extender as lme

from schemas import CombinedModel, DeltaModification, SemanticModelNode, ObjectProperty

os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"

app = FastAPI()


# cache to hold additional information for each URI: Format is uri -> {label, comment}
meta_info: Dict[str, Dict[str, str]] = {}

initialized: bool = False


# noinspection PyPep8Naming
# in order to match the API query parameter names used in Java components
@app.post("/recommendation")
async def recommend(combinedModel: CombinedModel, uuid: str = None, configId: str = None, configToken: str = None):
    global initialized
    if not initialized:
        init()
        initialized = True

    return generate_recommendations(combinedModel)


# noinspection PyPep8Naming
def generate_recommendations(combinedModel: CombinedModel) -> CombinedModel:
    count = 0

    for anchor in combinedModel.semanticModel.nodes:

        extensions = lme.generate_lme(anchor=anchor.uri, limit=5)
        recommendations = []
        for (s, p, o), c in extensions:
            delta: DeltaModification = DeltaModification()
            delta.reference = "reference-" + str(count)
            delta.anchors = [anchor]
            delta.confidence = 1.0
            count += 1
            # convert nodes
            label, comment = get_meta_info(o)
            o_smn = SemanticModelNode.parse_obj(
                {'_class': 'Class',
                 'uri': o,
                 'label': label,
                 'description': comment,
                 'uuid': str(uuid.uuid1())})

            delta.entities = [o_smn]

            # convert relation
            label, comment = get_meta_info(p)
            op: ObjectProperty = ObjectProperty.parse_obj(
                {'_class': 'ObjectProperty',  # TODO extend to support DatatypeProperties
                 'uri': p,
                 'label': label,
                 'description': comment,
                 'from': anchor.uuid,
                 'to': o_smn.uuid})
            delta.relations = [op]

            recommendations.append(delta)

        print("recommended for", anchor.uri, recommendations)
        for recommendation in recommendations:
            combinedModel.recommendations.append(recommendation)
    return combinedModel


def get_meta_info(uri: str):
    if uri not in meta_info:
        return None,None
    return meta_info[uri]['label'], meta_info[uri]['comment']


def init():
    global meta_info
    lme.init()
    meta_info = modelbuilder.modelbuilder.build_label_index()


if __name__ == "__main__":
    uvicorn.run("service:app", host="0.0.0.0", port=8000, reload=True)
