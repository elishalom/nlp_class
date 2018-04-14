import csv
import itertools
import math
from collections import defaultdict
from itertools import takewhile
from math import log
from os.path import basename, splitext

from tqdm import tqdm

from assignemnt_2.documents_reader import DocumentsReader
from assignemnt_2.model_base import ModelBase


class HMM(ModelBase):
    def __init__(self, order: int=1) -> None:
        super().__init__()
        self.order = order

    def train(self, train_file: str, smoothing: bool) -> None:
        lex_file = splitext(basename(train_file))[0] + '.lex'
        gram_file = splitext(basename(train_file))[0] + '.gram'

        em_probs = defaultdict(lambda: defaultdict(float))
        trans_probs = defaultdict(lambda: defaultdict(float))

        seg_pos_tuple_list = DocumentsReader.read(train_file)

        pos_prob = defaultdict(int)
        initial_vector = defaultdict(float)
        for sentence in seg_pos_tuple_list:
            last_pos = '<S>'
            initial_vector[sentence[0][1]] += 1

            for seg, pos in sentence:
                pos_prob[pos] += 1
                em_probs[seg][pos] += 1
                if last_pos is not None:
                    trans_probs[last_pos][pos] += 1
                last_pos = pos

        pos_counts = sum(pos_prob.values())
        delta_pos = 1 if smoothing else 0
        pos_prob = {k: log((v + delta_pos) / (pos_counts + delta_pos * pos_counts)) for k, v in pos_prob.items()}

        for pos in trans_probs.keys():
            adjacent_pos_counts = sum(trans_probs[pos].values())
            for adjacent_pos in trans_probs[pos]:
                trans_probs[pos][adjacent_pos] = log(trans_probs[pos][adjacent_pos] / adjacent_pos_counts)

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
            writer.writerows(map(reversed, pos_prob.items()))
            f.write('\n')
            f.write('\\2-grams\\\n')
            for pos, transitions in trans_probs.items():
                for adjacent_pos, log_proba in transitions.items():
                    writer.writerow([log_proba, pos, adjacent_pos])

    def decode(self, test_file, lex_file, grams_file):
        emition_probabilities = defaultdict(dict)
        with open(lex_file, 'r') as f:
            reader = csv.reader(f, delimiter='\t')
            for row in reader:
                segment = row[0]
                for pos, prob in zip(row[1::2], row[2::2]):
                    emition_probabilities[segment][pos] = float(prob)

        all_possible_pos = list(set(itertools.chain.from_iterable(emition_probabilities.values())))

        gram_order = 2
        transition_probabilities = {}
        with open(grams_file, 'rt') as f:
            list(takewhile(lambda line: line != '\\{}-grams\\\n'.format(gram_order), f))

            reader = csv.reader(f, delimiter='\t')
            for row in reader:
                transition_probabilities[tuple(row[1:])] = float(row[0])

        with open(splitext(basename(test_file))[0] + '.tagged', 'wt') as f:
            writer = csv.writer(f, delimiter='\t')
            for sentence in tqdm(DocumentsReader.read(test_file)):
                recent_pos = '<S>'
                mat = [[None] * (len(sentence)) for _ in range(len(all_possible_pos))]
                first_segment = sentence[0][0]
                for i, pos in enumerate(all_possible_pos):
                    mat[i][0] = (math.e ** transition_probabilities[(recent_pos, pos)]) if (recent_pos, pos) in transition_probabilities else 0
                    mat[i][0] += (math.e ** emition_probabilities[first_segment][pos]) if first_segment in emition_probabilities and pos in emition_probabilities[first_segment] else 0
                    mat[i][0] = math.log(mat[i][0]) if mat[i][0] > 0 else float('-inf')

                for j in range(1, len(sentence)):
                    segment = sentence[j][0]
                    for i, pos in enumerate(all_possible_pos):
                        mat[i][j] = (math.e ** emition_probabilities[segment][pos]) if first_segment in emition_probabilities and pos in emition_probabilities[segment] else 0
                        max_transition_probability = float('-inf')
                        for k, prev_pos in enumerate(all_possible_pos):
                            transition_probability = (math.e ** mat[k][j-1]) + (math.e ** transition_probabilities[(prev_pos, pos)]) if (prev_pos, pos) in transition_probabilities else 0
                            max_transition_probability = max(max_transition_probability, transition_probability)
                        mat[i][j] += max_transition_probability
                        mat[i][j] = math.log(mat[i][j])

                pos_positions = [max(range(len(all_possible_pos)), key=lambda i: mat[i][j]) for j in range(len(sentence))]

                segments_tags = [(part[0], all_possible_pos[pos_position]) for part, pos_position in
                                 zip(sentence, pos_positions)]

                writer.writerows(segments_tags)
                f.write('\n')
