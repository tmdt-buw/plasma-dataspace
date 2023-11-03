import codecs
import json
import os


class HistoryProcessing:
    HISTORY_FILE = 'vcslam_history.json'

    mappings = None

    def __init__(self):
        self.mappings = {}
        self.load_history()

    def reset(self):
        self.mappings = None

    def load_history(self):
        with codecs.open(self.HISTORY_FILE, encoding='utf8', mode='r') as f:
            self.mappings = json.load(f)

    def get_history_mapping(self, attribute):

        if attribute in self.mappings:
            return [[att, 1] for att in self.mappings[attribute]]
        return []
