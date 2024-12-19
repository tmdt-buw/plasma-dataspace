from apscheduler.triggers.interval import IntervalTrigger
from fastapi import FastAPI
from datetime import datetime
from contextlib import asynccontextmanager
from apscheduler.schedulers.background import BackgroundScheduler  # runs tasks in the background
from apscheduler.triggers.cron import CronTrigger  # allows us to specify a recurring time for execution


registration_scheduler = BackgroundScheduler()


def registration_task():
    print(f"Registration task is running at {datetime.now()}")
    # ... additional task code goes here ...


def start_registration_schedule():
    registration_trigger = IntervalTrigger(minutes=1)
    registration_scheduler.add_job(registration_task, registration_trigger)
    registration_scheduler.start()
    print(f"Started registration scheduler...")


def stop_registration_schedule():
    registration_scheduler.shutdown()


