"""Exercise the HTTP contract without training or registering a live recommender."""
from unittest.mock import patch
from fastapi.testclient import TestClient
import service


def model(*uris):
    return {"semanticModel": {"nodes": [
        {"_class": "Class", "uuid": f"node-{index}", "uri": uri, "x": 10, "y": 20}
        for index, uri in enumerate(uris)], "edges": []}}


def post(payload, extensions, initialized=True):
    # No lifespan context: these tests must never start the scheduled training/registration jobs.
    with patch.object(service, "initialized", initialized), patch.object(service.lme, "generate_lme", side_effect=extensions):
        return TestClient(service.app).post("/recommendation", json=payload)


def test_waits_for_initialization():
    response = post(model("urn:Station"), lambda **kwargs: [], initialized=False)
    assert response.status_code == 200
    assert response.json() == []


def test_keeps_recommendations_for_each_anchor_and_preserves_types():
    response = post(model("urn:Station", "urn:Sensor"),
                    lambda anchor, **kwargs: [((anchor, "urn:observes", "urn:Temperature"), 0.9)])
    assert response.status_code == 200
    items = response.json()
    assert [item["anchors"] for item in items] == [["node-0"], ["node-1"]]
    assert len({item["reference"] for item in items}) == 2
    for item in items:
        assert item["entities"][0]["_class"] == "Class"
        assert item["relations"][0]["_class"] == "ObjectProperty"
        assert item["relations"][0]["to"] == item["entities"][0]["uuid"]
        assert item["confidence"] == 0.9


def test_returns_a_literal_and_data_property_for_datatypes_without_coordinates():
    payload = model("urn:Station")
    payload["semanticModel"]["nodes"][0].pop("x")
    payload["semanticModel"]["nodes"][0].pop("y")
    response = post(payload, lambda anchor, **kwargs: [((anchor, "urn:value", "http://www.w3.org/2001/XMLSchema#decimal"), 0.8)])
    assert response.status_code == 200
    item = response.json()[0]
    assert item["entities"][0]["_class"] == "Literal"
    assert item["relations"][0]["_class"] == "DataProperty"


def test_existing_relations_are_not_suggested_again():
    payload = model("urn:Station", "urn:Temperature")
    payload["semanticModel"]["edges"] = [{"_class": "ObjectProperty", "from": "node-0", "to": "node-1", "uri": "urn:observes"}]
    response = post(payload, lambda anchor, **kwargs: [((anchor, "urn:observes", "urn:Temperature"), 0.9)] if anchor == "urn:Station" else [])
    assert response.status_code == 200
    assert response.json() == []


def test_empty_model_is_safe():
    response = post({}, lambda **kwargs: [])
    assert response.status_code == 200
    assert response.json() == []


def test_filters_internal_properties_and_limits_results():
    def extensions(anchor, **kwargs):
        return [((anchor, "http://plasma.uni-wuppertal.de/cm#private", "urn:internal"), 1)] + [
            ((anchor, "urn:observes", f"urn:Temperature-{i}"), 0.9) for i in range(8)]
    response = post(model("urn:Station"), extensions)
    assert response.status_code == 200
    assert len(response.json()) == 5
    assert all(item["relations"][0]["uri"] == "urn:observes" for item in response.json())
