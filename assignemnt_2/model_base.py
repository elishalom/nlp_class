import csv
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
    def evaluate(model, tagged_file, gold_file, smoothing):
        results_header = """# ------------------------
# Part-of-Speech Tagging Evaluation
# ------------------------
# Model: {}
# Smoothing: {}
# Test File: : {}
# Gold File: : {}
#
# ------------------------
# sent-num word-accuracy sent-accuracy
# ------------------------
""".format(model, smoothing, tagged_file, gold_file)
        with open('results.eval', 'wt') as f:
            writer = csv.writer(f, delimiter='\t')
            f.write(results_header)

            tagged = list(DocumentsReader.read(tagged_file))
            gold = list(DocumentsReader.read(gold_file))

            evaluated_sentences = [[tag[1] == gold[1] for tag, gold in zip(tagged_sent, gold_sent)]
                                   for tagged_sent, gold_sent in zip(tagged, gold)]

            segments_accuracy_all = 0
            total_num_of_segments = 0
            sentence_accuracy_all = 0
            for i, evaluated_sentence in enumerate(evaluated_sentences):
                num_of_correct_predictions = sum(evaluated_sentence)
                num_of_segments_in_sentence = len(evaluated_sentence)

                segments_accuracy = num_of_correct_predictions / num_of_segments_in_sentence
                sentence_accuracy = int(all(evaluated_sentence))

                sentence_accuracy_all += sentence_accuracy

                segments_accuracy_all += num_of_correct_predictions
                total_num_of_segments += num_of_segments_in_sentence

                writer.writerow([i, segments_accuracy, sentence_accuracy])

            f.write("""# ------------------------
macro-avg   {}  {}""".format(segments_accuracy_all / total_num_of_segments, sentence_accuracy_all / len(evaluated_sentences)))
