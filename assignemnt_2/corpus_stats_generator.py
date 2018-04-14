from os.path import join, dirname

from assignemnt_2.tagged_data_reader import TaggedDataLoader
from assignemnt_2.tagged_corpus_stats_generator import TaggedCorpusStatsGenerator
from assignemnt_2.tagged_data_normalizer import TaggedDataNormalizer


class CorpusStatsGenerator(object):
    @staticmethod
    def generate(source_file):
        df = TaggedDataLoader(TaggedDataNormalizer()).load(source_file)
        print(TaggedCorpusStatsGenerator().generate(df))


if __name__ == '__main__':
    CorpusStatsGenerator.generate(join(dirname(__file__), 'data-files', 'heb-pos.gold'))