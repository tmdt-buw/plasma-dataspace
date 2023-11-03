import re

delimiters = ['.', ',', ' ', '_', '-', ';', ':', '?', '[', ']']
regexPattern = '|'.join(map(re.escape, delimiters))


class DataProcessing:
    nlp = None
    attributes = []
    data = {}
    text = ''

    def __init__(self, nlp, external_description, attributes):
        self.reset()

        self.nlp = nlp
        self.text = external_description.lower()
        self.lemmas = []
        self.attributes = attributes

        self.load_data_external()

    def reset(self):
        self.nlp = None
        self.attributes = []
        self.data = {}
        self.text = ''

    def get_data(self):
        return self.data

    def set_data(self, data):
        self.data = data

    def get_text(self):
        return self.text

    def get_lemmas(self):
        return self.lemmas

    def load_data_external(self):

        for token in self.nlp(self.text):
            if token.pos_ == 'NOUN':
                self.lemmas.append(token.lemma_)

        for attribute in self.attributes:
            self.data[attribute] = {
                'lemma': '',
                'components': [],
                'component-lemmas': [],
                'sample_values': [],
                'candidates': {
                    'wiki_embeddings': [],
                    'vcslam_embeddings': [],
                    'identity_matches': [],
                    'similarity_matches': [],
                    'partial_matches': [],
                    'close_in_text': []
                }
            }
            doc = self.nlp(attribute)
            try:
                self.data[attribute]['lemma'] = doc[0].lemma_
            except:
                self.data[attribute]['lemma'] = None
            self.data[attribute]['components'] = re.split(regexPattern, attribute)
            doc = self.nlp(' '.join(self.data[attribute]['components']))
            for token in doc:
                if token.pos_ == 'NOUN':
                    self.data[attribute]['component-lemmas'].append(token.lemma_)

    def get_close_words(self, word, window_size):
        if word not in self.text:
            return []

        words = self.nlp(self.text)

        seen_indices = []
        identified_tokens = []
        for index, token in enumerate(words):
            if str(token) == word and not set(seen_indices) & set(range(index - window_size, index + window_size)):
                seen_indices.append(index)
                sublist = words[index - window_size:index + window_size]
                identified_tokens += [str(w) for w in sublist if w.pos_ in ['NOUN', 'PROPN', 'VERB'] and not w.is_stop]
        return list(set(identified_tokens))

    def get_close_sentences(self, word):
        if word not in self.text:
            return []

        doc = self.nlp(self.text)
        result = []

        for s in doc.sents:
            if word in str(s):
                result.append(str(s))
        return result
