FROM python:3.9

WORKDIR /app
COPY requirements.txt requirements.txt

RUN pip3 install -r requirements.txt

COPY data/*_history.json data
COPY *.py .

RUN python -m spacy download en_core_web_sm

CMD [ "python3", "service.py"]