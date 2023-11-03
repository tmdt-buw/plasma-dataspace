from pipeline_manager import PipelineManager
import codecs
import json

if __name__ == '__main__':
    pm = PipelineManager()

    with codecs.open('data/data_example.json', encoding='utf8', mode='r') as f:
        data = json.load(f)
        results = pm.run_pipeline(data['description'], data['attributes'])

        print(results)