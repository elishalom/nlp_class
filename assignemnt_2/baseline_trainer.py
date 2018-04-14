import csv
from abc import abstractmethod, ABCMeta
from os.path import splitext

import pandas as pd

from assignemnt_2.documents_reader import DocumentsReader


class ModelBase(object, metaclass=ABCMeta):
    @abstractmethod
    def train(self, train_corpus: pd.DataFrame):
        pass

    @abstractmethod
    def decode(self, test_file):
        pass

    @abstractmethod
    def evaluate(self, tagged_file, gold_file):
        pass


class BaselineTrainer(ModelBase):
    def __init__(self) -> None:
        super().__init__()
        self._occurrences_per_tag = None

    def train(self, train_corpus: pd.DataFrame) -> pd.DataFrame:
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

    def evaluate(self, tagged_file, gold_file):
        tagged = list(DocumentsReader.read(tagged_file))
        gold = list(DocumentsReader.read(gold_file))

        evaluated_sentences = [[tag[1] == gold[1] for tag, gold in zip(tagged_sent, gold_sent)]
                               for tagged_sent, gold_sent in zip(tagged, gold)]

        correct_sentences = sum(1 for evaluated_sentence in evaluated_sentences if all(evaluated_sentence))
        correct_segments = sum(sum(evaluated_sentence) for evaluated_sentence in evaluated_sentences)

        num_of_sentences = len(evaluated_sentences)
        num_of_segments = sum(map(len, evaluated_sentences))
        sentence_accuracy = correct_sentences / num_of_sentences
        segments_accuracy = correct_segments / num_of_segments

        print('Sentence Accuracy = {}, Segments Accuracy = {}'.format(sentence_accuracy, segments_accuracy))



