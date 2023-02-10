import os
import uuid
from typing import List
from uuid import UUID

import uvicorn
from fastapi import FastAPI
from pydantic import BaseModel

from linkedmodelextension import linked_model_extender as lme

from schemas import CombinedModel, DeltaModification, SemanticModelNode, ObjectProperty

os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"

app = FastAPI()


# noinspection PyPep8Naming
# in order to match the API query parameter names used in Java components
@app.post("/recommendation")
async def recommend(combinedModel: CombinedModel, uuid: str = None, configId: str = None, configToken: str = None):
    return generate_recommendations(combinedModel)


# noinspection PyPep8Naming
def generate_recommendations(combinedModel: CombinedModel) -> CombinedModel:
    count = 0

    for anchor in combinedModel.semanticModel.nodes:

        extensions = lme.generate_lme(anchor=anchor.uri)
        recommendations = []
        for (s, p, o), c in extensions:
            delta: DeltaModification = DeltaModification()
            delta.reference = "reference-" + str(count)
            delta.anchors = [anchor]
            delta.confidence = 1.0
            count += 1
            # convert nodes
            smn = SemanticModelNode.parse_obj(
                {'_class': 'Class',
                 'uri': s,
                 'uuid': str(uuid.uuid1())})
            delta.entities = [smn]

            # convert relation
            op: ObjectProperty = ObjectProperty.parse_obj(
                {'_class': 'ObjectProperty',  # TODO extend to support DatatypeProperties
                 'uri': p,
                 'from': anchor.uuid,
                 'to': smn.uuid})
            delta.relations = [op]
            recommendations.append(delta)

        print("recommended for", anchor.uri, recommendations)
        for recommendation in recommendations:
            combinedModel.recommendations.append(recommendation)
    return combinedModel


def init():
    lme.init()


if __name__ == "__main__":
    init()
    uvicorn.run("service:app", host="0.0.0.0", port=8000, reload=True)
