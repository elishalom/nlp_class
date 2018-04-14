from os.path import join, dirname

from assignemnt_2.default_tagged_data_load import DefaultTaggedDataLoader
from assignemnt_2.tagged_corpus_stats_generator import TaggedCorpusStatsGenerator


class CorpusStatsGenerator(object):
    @staticmethod
    def generate(source_file):
        df = DefaultTaggedDataLoader.load(source_file)
        print(TaggedCorpusStatsGenerator().generate(df))


if __name__ == '__main__':
    CorpusStatsGenerator.generate(join(dirname(__file__), 'data-files', 'heb-pos.gold'))