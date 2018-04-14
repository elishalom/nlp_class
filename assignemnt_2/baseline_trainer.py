import csv
from collections import defaultdict, Counter
from itertools import chain
from os.path import splitext, basename

from assignemnt_2.documents_reader import DocumentsReader
from assignemnt_2.model_base import ModelBase


class BaselineTrainer(ModelBase):
    def __init__(self, tags_file: str = None) -> None:
        super().__init__()
        if tags_file is None:
            self._segment_to_tag = None
        else:
            with open(tags_file, 'rt') as f:
                reader = csv.reader(f, delimiter='\t')
                self._segment_to_tag = dict(reader)

    def train(self, train_file: str):
        reader = DocumentsReader.read(train_file)
        segment_to_tag_counts = defaultdict(Counter)
        for segment, tag in chain.from_iterable(reader):
            segment_to_tag_counts[segment][tag] += 1

        segment_to_tag = {segment: max(counts.keys(), key=counts.get) for segment, counts in
                          segment_to_tag_counts.items()}

        with open('segment_to_tag.tsv', 'wt') as f:
            writer = csv.writer(f, delimiter='\t')
            writer.writerows(sorted(segment_to_tag.items()))

    def decode(self, test_file):
        with open(test_file, 'rt') as sf, open(splitext(basename(test_file))[0] + '.tagged', 'wt') as tf:
            writer = csv.writer(tf, delimiter='\t')
            for segment in sf:
                segment = segment.strip()
                if segment == '':
                    writer.writerow([])
                else:
                    writer.writerow([segment, self._segment_to_tag.get(segment, 'NNP')])
