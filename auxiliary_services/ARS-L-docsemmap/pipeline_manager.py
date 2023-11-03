import operator

from DataProcessing import DataProcessing
from FlairProcessing import FlairProcessing
from HistoryProcessing import HistoryProcessing
from OntologyProcessing import OntologyProcessing
import spacy
from Levenshtein import distance

from WikipediaProcessing import WikipediaProcessing


def score_by_levenshtein_and_length(s1, s2):
    length = max(len(s1), len(s2))
    dist = float(float(length - distance(s1, s2)) / float(length))
    return dist


class PipelineManager:
    spacyInstance = None
    flairProcessing = None
    ontologyProcessing = None
    historyProcessing = None
    wikipediaProcessing = None

    matching_weights = {
        'close_aix': 1.05,
        'similar_wiki_aix': 0.5,
        'partial_match_aix': 1.45,
        'similar_desc_aix': 1.95,
        'similar_wiki_sec_aix': 1.0,
        'history': 1.25
    }

    def __init__(self):
        self.spacyInstance = spacy.load("en_core_web_sm")
        self.flairProcessing = FlairProcessing()
        self.ontologyProcessing = OntologyProcessing(te_spacy=self.spacyInstance,
                                                     te_bert=self.flairProcessing)
        self.historyProcessing = HistoryProcessing()

        self.wikipediaProcessing = WikipediaProcessing()

    # The following method, runs the pipeline on a given description and a list of attributes
    # It returns a list of concepts that are found in the description
    def run_pipeline(self, description, attributes):
        data_processing = DataProcessing(nlp=self.spacyInstance,
                                         external_description=description,
                                         attributes=attributes)

        labels = data_processing.get_data().keys()

        candidates = {}
        results = {}

        for aix in labels:
            candidate = self.label_processing(aix=aix, data_processing=data_processing,
                                              te_bert=self.flairProcessing,
                                              ontology=self.ontologyProcessing,
                                              history_processing=self.historyProcessing)
            candidates[aix] = candidate
            result = self.matching(candidate)
            if result != '':
                results[aix] = (result, self.ontologyProcessing.get_uri_for_label(result))

        return results

    def label_processing(self, aix: str, data_processing: DataProcessing,
                         te_bert: FlairProcessing,
                         ontology: OntologyProcessing,
                         history_processing: HistoryProcessing):

        candidates = {}

        close_aix = data_processing.get_close_words(aix, 8)

        close_aix = [(x, 0.5) for x in close_aix]

        similar_wiki_aix = self.wikipediaProcessing.get_similar_by_wiki(aix, 20)
        history_aix = history_processing.get_history_mapping(aix)

        partial_match_aix = self.perform_partial_match(aix, data_processing, ontology)

        similar_desc_aix = []

        close_sentences = [sent for sent in data_processing.get_close_sentences(aix) if len(sent.split(' ')) > 4]

        for concept in ontology.get_concepts().keys():
            for sentence in close_sentences:
                sentence_vector = te_bert.get_vector(sentence)
                if 'descriptionVector' in ontology.get_concepts()[concept]:
                    similarity = te_bert.get_cosine_similarity_of_vectors(sentence_vector,
                                                                          ontology.get_concepts()[concept][
                                                                              'descriptionVector'])
                    if similarity >= 0.85:
                        similar_desc_aix.append((concept, similarity))

        # prepare result dictionary

        similar_wiki_sec_aix = []

        wiki_desc = self.wikipediaProcessing.get_wikipedia_description(aix)
        if wiki_desc != '':
            vector_wiki_desc = te_bert.get_vector(wiki_desc)
            for con in ontology.get_concepts().keys():
                if 'descriptionVector' in ontology.get_concepts()[con]:
                    similarity = te_bert.get_cosine_similarity_of_vectors(vector_wiki_desc,
                                                                          ontology.get_concepts()[con][
                                                                              'descriptionVector'])
                    if similarity >= 0.85:
                        similar_wiki_sec_aix.append((con, similarity))

        candidates['close_aix'] = ontology.reduce_to_known_concepts_with_ratings(close_aix)
        candidates['similar_wiki_aix'] = ontology.reduce_to_known_concepts_with_ratings(similar_wiki_aix)
        candidates['partial_match_aix'] = ontology.reduce_to_known_concepts_with_ratings(partial_match_aix)
        candidates['similar_wiki_sec_aix'] = ontology.reduce_to_known_concepts_with_ratings(similar_wiki_sec_aix)
        candidates['history'] = ontology.reduce_to_known_concepts_with_ratings(history_aix)

        return candidates

    def perform_partial_match(self, aix, data_processing, ontology):
        attribute_components = [aix]

        for split_pattern in ['_', '-', '#', ':']:
            splitted = aix.split(split_pattern)
            if len(splitted) > 1:
                attribute_components += splitted

        attribute_components += data_processing.get_data()[aix]['component-lemmas']

        attribute_components = list(set([a.lower() for a in attribute_components]))

        results = ontology.reduce_to_known_concepts_partial_matchs(attribute_components)
        return [[x, score_by_levenshtein_and_length(aix.lower(), x.lower())] for x in results]

    def matching(self, candidates_aix):
        match_dict = {}

        for type in candidates_aix.keys():
            self.update_match_by_list(match_dict, candidates_aix[type], type)

        try:
            return max(match_dict.items(), key=operator.itemgetter(1))[0]
        except:
            return ''

    def update_match_by_list(self, match_dict, lst, type_name: str):
        for entry in lst:
            if entry[0] in match_dict:
                match_dict[entry[0]] += entry[1] * self.matching_weights[type_name]
            else:
                match_dict[entry[0]] = entry[1] * self.matching_weights[type_name]
