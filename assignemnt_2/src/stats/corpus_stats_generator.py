from itertools import chain
from operator import attrgetter
from os.path import join

from helpers.directories import EXPERIMENTS_DIR
from documents_reading.documents_reader import DocumentsReader


class CorpusStatsGenerator(object):
    @staticmethod
    def generate(source_file):
        tagged_sentances = list(DocumentsReader().read(source_file))
        num_unigrams_instances = num_tags_instances = sum(map(len, tagged_sentances))
        num_unigrams = len(set(map(attrgetter('segment'), chain.from_iterable(tagged_sentances))))
        num_tags = len(set(chain.from_iterable(tagged_sentances)))

        print(num_unigrams, num_unigrams_instances)
        print(num_tags, num_tags_instances)
        print(num_tags / num_unigrams)


if __name__ == '__main__':
    CorpusStatsGenerator.generate(join(EXPERIMENTS_DIR, 'heb-pos.train'))
