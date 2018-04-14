import pandas as pd

from assignemnt_2.corpus_stats import CorpusStats


class TaggedCorpusStatsGenerator(object):
    def generate(self, tagged_corpus: pd.DataFrame) -> CorpusStats:
        num_of_unigram_instances = tagged_corpus['segment'].count()
        num_of_unigram = tagged_corpus['segment'].drop_duplicates().count()
        num_of_types_instances = tagged_corpus['type'].count()
        num_of_types = tagged_corpus['type'].drop_duplicates().count()

        return CorpusStats(num_of_unigram_instances,
                           num_of_unigram,
                           num_of_types_instances,
                           num_of_types)