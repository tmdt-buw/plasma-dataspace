import wikipediaapi
import gensim.downloader as api

class WikipediaProcessing:

    def __init__(self):
        try:
            self.wiki2vec = api.load("glove-wiki-gigaword-100")
        except Exception as ex:
            self.wiki2vec = None

    def get_similar_by_wiki(self, term, top_n=10):
        try:
            if self.wiki2vec:
                word_vector = self.wiki2vec.get_word_vector(term)
                similar_words = self.wiki2vec.most_similar(word_vector, top_n=top_n)
                return [(word.text, score) for word, score in similar_words]
            else:
                return []
        except Exception as ex:
            return []

    def get_wikipedia_description(self, term):
        try:
            wiki_wiki = wikipediaapi.Wikipedia('en')
            page = wiki_wiki.page(term)
            if page.exists():
                return page.summary
            else:
                return f"No Wikipedia page found for '{term}'."
        except Exception as ex:
            return ''
