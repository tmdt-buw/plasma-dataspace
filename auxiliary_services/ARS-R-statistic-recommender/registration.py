import json
import logging
import os
from enum import Enum
import requests
from apscheduler.triggers.interval import IntervalTrigger
from datetime import datetime
from apscheduler.schedulers.background import BackgroundScheduler  # runs tasks in the background

import uuid as uuid_lib
uuid = str(uuid_lib.uuid4())

class RecommendationServiceRegistration:
    class RecommendationType(Enum):
        LABELING = "Labeling"
        REFINEMENT = "Refinement"
        MODELING = "Modeling"

    def __init__(self, service_name: str, url: str, last_active: datetime, uuid: str, recommendation_type: RecommendationType, description: str):
        self.service_name = service_name
        self.url = url
        self.last_active = last_active
        self.uuid = uuid
        self.recommendation_type = recommendation_type
        self.description = description

    def __repr__(self):
        return (
            f"RecommendationServiceRegistration(service_name={self.service_name}, "
            f"url={self.url}, last_active={self.last_active}, uuid={self.uuid}, "
            f"recommendation_type={self.recommendation_type}, description={self.description})"
        )

    def to_dict(self):
        """
        Converts the instance to a dictionary suitable for JSON serialization.

        Returns:
            dict: A dictionary representation of the instance.
        """
        return {
            "serviceName": self.service_name,
            "url": self.url,
            "lastActive": self.last_active.isoformat(),  # Convert datetime to ISO 8601 string
            "uuid": self.uuid,
            "recommendationType": self.recommendation_type.value,  # Use the enum's value
            "description": self.description
        }


registration_scheduler = BackgroundScheduler()

def post_to_server(url, data):
    """
    Makes a POST request to the given URL with the specified data.

    Args:
        url (str): The URL to send the POST request to.
        data (dict): The payload to include in the POST request.

    Returns:
        dict: The JSON response from the server if successful.
    """
    try:
        # Make the POST request
        response = requests.post(url, json=data)
        # Check if the request was successful
        response.raise_for_status()

    except requests.exceptions.RequestException as e:
        print(f"An error occurred while making the POST request: {e}")
        raise

def registration_task():
    if "PLASMA_REGISTRATION_URL" not in os.environ:
        logging.error("Could not find PLASMA ARS registration URL")
        return

    if "PLASMA_ARS_API_URL" not in os.environ:
        logging.error("Could not find ARS service URL")
        return

    service = RecommendationServiceRegistration(
        service_name="Statistical Recommender",
        url=os.getenv("PLASMA_ARS_API_URL"),
        last_active=datetime.now(),
        uuid=uuid,
        recommendation_type=RecommendationServiceRegistration.RecommendationType.REFINEMENT,
        description="SR Service"
    )

    url = os.getenv("PLASMA_REGISTRATION_URL")
    payload = service.to_dict()

    try:
        print(f"posting to {url}")
        result = post_to_server(url, payload)
        # print("POST request succeeded. Response:", result)
    except Exception as e:
        print("Failed to complete the POST request:", e)



def start_registration_schedule():
    registration_trigger = IntervalTrigger(minutes=1)
    registration_scheduler.add_job(registration_task, registration_trigger)
    registration_scheduler.start()
    print(f"Started registration scheduler...")


def stop_registration_schedule():
    registration_scheduler.shutdown()


