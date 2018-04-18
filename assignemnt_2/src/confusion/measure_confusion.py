from collections import Counter
from itertools import chain
from operator import attrgetter
from os.path import join

from documents_reading.documents_reader import DocumentsReader
from helpers.directories import EXPERIMENTS_DIR


def measure(test_file, tagged_file):
    reader = DocumentsReader()
    test_tags = map(attrgetter('pos'), chain.from_iterable(reader.read(test_file)))
    predicted_tags = map(attrgetter('pos'), chain.from_iterable(reader.read(tagged_file)))

    confusion_matrix = Counter(zip(test_tags, predicted_tags))
    mistakes_matrix = Counter({(expected, predicted): counts for (expected, predicted), counts in confusion_matrix.items() if expected != predicted})
    print(mistakes_matrix.most_common(3))


def main():
    test_file = join(EXPERIMENTS_DIR, 'heb-pos.gold')
    tagged_file = join(EXPERIMENTS_DIR, 'hmm_2_with_smoothing_heb-pos.tagged')
    measure(test_file, tagged_file)


if __name__ == '__main__':
    main()
