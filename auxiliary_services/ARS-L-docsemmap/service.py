import requests
import uvicorn
from fastapi import FastAPI
import os
import uuid

from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
from pipeline_manager import PipelineManager
from schemas import CombinedModel, RecommendationRequest, SemanticModel, SemanticModelNode, MappableSemanticModelNode


class LabelRequest(BaseModel):
    description: str
    attributes: list


os.environ["SERVICE_PORT"] = "8221"

app = FastAPI()
app.pipeline_manager = None


@app.on_event("startup")
async def initialize():
    app.pipeline_manager = PipelineManager()
    print("INFO", "Pipeline manager initialized.")


@app.post("/predict")
async def predict(label_request: LabelRequest):
    if app.pipeline_manager:
        results = app.pipeline_manager.run_pipeline(label_request.description, label_request.attributes)
        return results
    return []


@app.post("/recommendation")
async def recommend(recommendationRequest: RecommendationRequest, configId: str = None, configToken: str = None):
    return generate_recommendation(recommendationRequest.combinedModel, recommendationRequest.uuid)


def generate_recommendation(combinedModel: CombinedModel, uuid: str = None) -> CombinedModel:
    endpoint = 'http://localhost:8888/api/plasma-dms/modelings/'  # TODO: add as config
    modelings = requests.get(endpoint).json()

    for model in modelings:
        if model['id'] == uuid:
            description = model['description']
            break

    attributes = extract_attributes_from_model(combinedModel)

    if app.pipeline_manager:
        results = app.pipeline_manager.run_pipeline(description, attributes)

        return update_combined_model(combinedModel, results)
    return []


def fetch_data_source_details():
    pass


def extract_attributes_from_model(combinedModel: CombinedModel) -> [str]:
    attributes = []
    for node in combinedModel.syntaxModel.nodes:
        if node.field_class == 'PrimitiveNode':
            attributes.append(node.label)
    return attributes


def update_combined_model(combinedModel: CombinedModel, mappings: dict):
    if not combinedModel.semanticModel:
        combinedModel.semanticModel = SemanticModel(nodes=[], edges=[])

    for key in mappings.keys():
        for syntaxNode in combinedModel.syntaxModel.nodes:
            if syntaxNode.label == key:
                semanticNode = MappableSemanticModelNode(
                    uuid=str(uuid.uuid4()),
                    syntaxNodeUuid=syntaxNode.uuid,
                    syntaxLabel=syntaxNode.label,
                    syntaxPath=syntaxNode.pathAsJSONPointer,
                    mapped=True,
                    uri=mappings[key][1],
                    label=mappings[key][0],
                    _class='Class'
                )
                combinedModel.semanticModel.nodes.append(semanticNode)
                break

    return combinedModel


if __name__ == "__main__":
    port: int = int(os.getenv("SERVICE_PORT"))
    if not port:
        print("INFO", "No service port specified via 'SERVICE_PORT' env variable. Defaulting to 8221.")
        port = 8222
    uvicorn.run("service:app", host="0.0.0.0", port=port, reload=True)
