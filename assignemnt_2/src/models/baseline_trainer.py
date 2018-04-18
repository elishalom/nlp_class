import csv
from collections import defaultdict, Counter
from itertools import chain
from os.path import splitext, basename, join

from helpers.directories import EXPERIMENTS_DIR
from documents_reading.documents_reader import DocumentsReader
from models.model_base import ModelBase


class BaselineTrainer(ModelBase):
    def __init__(self) -> None:
        super().__init__()
        self.__param_files = join(EXPERIMENTS_DIR, 'segment_to_tag.tsv')

    def train(self, train_file: str, *args):
        reader = DocumentsReader.read(train_file)
        segment_to_tag_counts = defaultdict(Counter)
        for segment, tag in chain.from_iterable(reader):
            segment_to_tag_counts[segment][tag] += 1

        segment_to_tag = {segment: max(counts.keys(), key=counts.get) for segment, counts in
                          segment_to_tag_counts.items()}

        with open(self.__param_files, 'wt') as f:
            writer = csv.writer(f, delimiter='\t')
            writer.writerows(sorted(segment_to_tag.items()))

    def decode(self, test_file):
        with open(self.__param_files, 'rt') as f:
            reader = csv.reader(f, delimiter='\t')
            segment_to_tag = dict(reader)

        with open(test_file, 'rt') as sf, open(join(EXPERIMENTS_DIR, splitext(basename(test_file))[0] + '.tagged'), 'wt') as tf:
            writer = csv.writer(tf, delimiter='\t')
            for segment in sf:
                segment = segment.strip()
                if segment == '':
                    writer.writerow([])
                else:
                    writer.writerow([segment, segment_to_tag.get(segment, 'NNP')])
