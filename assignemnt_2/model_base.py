from abc import ABCMeta, abstractmethod

import pandas as pd

from assignemnt_2.documents_reader import DocumentsReader


class ModelBase(object, metaclass=ABCMeta):
    @abstractmethod
    def train(self, train_file: str):
        pass

    @abstractmethod
    def decode(self, test_file):
        pass

    @staticmethod
    def evaluate(tagged_file, gold_file):
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