import csv
from os.path import splitext

import pandas as pd

from assignemnt_2.documents_reader import DocumentsReader
from assignemnt_2.model_base import ModelBase


class BaselineTrainer(ModelBase):
    def __init__(self) -> None:
        super().__init__()
        self._occurrences_per_tag = None

    def train(self, train_file: str):
        reader = DocumentsReader(train_file)

        occurrences_per_tag = train_corpus.groupby(['segment', 'tag']).size().to_frame('occurrences')
        max_tag = occurrences_per_tag.groupby('segment')['occurrences'].max().to_frame('max_occurrences')
        occurrences_per_tag = occurrences_per_tag.join(max_tag)
        occurrences_per_tag = occurrences_per_tag.loc[occurrences_per_tag['occurrences'] == occurrences_per_tag['max_occurrences']]
        occurrences_per_tag = occurrences_per_tag.loc[~occurrences_per_tag.index.duplicated()]
        occurrences_per_tag = occurrences_per_tag[[]].reset_index('tag')
        self._occurrences_per_tag = occurrences_per_tag

    def persist(self, target_file: str) -> None:
        self._occurrences_per_tag.to_csv(target_file, sep='\t')

    def load(self, tags_file: str) -> None:
        self._occurrences_per_tag = pd.read_csv(tags_file, sep='\t', index_col='segment', na_filter=None)

    def decode(self, test_file):
        tags = self._occurrences_per_tag['tag'].to_dict()

        with open(test_file, 'rt') as sf, open(splitext(test_file)[0] + '.tagged', 'wt') as tf:
            writer = csv.writer(tf, delimiter='\t')
            for segment in sf:
                segment = segment.strip()
                if segment == '':
                    writer.writerow([])
                else:
                    writer.writerow([segment, tags.get(segment, 'NNP')])



