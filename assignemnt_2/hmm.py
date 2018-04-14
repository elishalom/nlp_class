import csv
import itertools
import os
from collections import defaultdict
from math import log
from os.path import basename, splitext

from assignemnt_2.baseline_trainer import ModelBase
from assignemnt_2.documents_reader import DocumentsReader


class HMM(ModelBase):
    def __init__(self, order: int=1) -> None:
        super().__init__()
        self.order = order

    def train(self, train_file: str):
        lex_file = splitext(basename(train_file))[0] + '.lex'
        gram_file = splitext(basename(train_file))[0] + '.gram'

        em_probs = defaultdict(lambda : defaultdict(float))
        trans_probs = defaultdict(lambda : defaultdict(float))

        seg_pos_tuple_list = DocumentsReader.read(train_file)

        initial_vector = defaultdict(float)
        for sentence in seg_pos_tuple_list:
            last_pos = None
            initial_vector[sentence[0][1]] += 1
            for seg, pos in sentence:
                em_probs[seg][pos] += 1
                if last_pos is not None:
                    trans_probs[last_pos][pos] += 1
                last_pos = pos

        num_of_initial_tags = sum(initial_vector.values())
        initial_vector = {k: log(v/num_of_initial_tags) for k, v in initial_vector.items()}

        for seg in em_probs.keys():
            segment_tags = sum(em_probs[seg].values())
            for pos in em_probs[seg]:
                em_probs[seg][pos] = log(em_probs[seg][pos] / segment_tags)

        with open(lex_file, 'w') as f:
            writer = csv.writer(f, delimiter='\t')
            for segment, pos_log_proba in em_probs.items():
                writer.writerow([segment] + list(itertools.chain.from_iterable(sorted(pos_log_proba.items()))))

        with open(gram_file, 'w') as f:
            f.write('\\data\\\n')
            f.write('ngram 1 = {}\n'.format(len(em_probs)))
            f.write('\n')
            f.write('\\1-grams\\\n')
            writer = csv.writer(f, delimiter='\t')



    def decode(self, test_file):
        pass

    def evaluate(self, tagged_file, gold_file):
        pass
