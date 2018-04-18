import csv
import math
from collections import defaultdict, Counter
from itertools import takewhile, chain, groupby, product
from os.path import basename, splitext, join
from typing import Dict, Tuple, List

from tqdm import tqdm

from helpers.directories import EXPERIMENTS_DIR
from documents_reading.documents_reader import DocumentsReader
from models.model_base import ModelBase

UNKNOWN = '!UNKNOWN!'


class HMM(ModelBase):
    def __init__(self, order: int=1) -> None:
        super().__init__()
        self.__order = order

    def train(self, train_file: str, smoothing: bool= True) -> None:
        seg_pos_tuple_list = list(DocumentsReader.read(train_file))

        tags_counts = Counter(part.pos for sentence in seg_pos_tuple_list for part in sentence)
        tags_counts['<S>'] = len(seg_pos_tuple_list)
        all_segments = set(part.segment for sentence in seg_pos_tuple_list for part in sentence)
        delta = 0.01 if smoothing else 0
        v = len(all_segments) * delta

        num_of_unigrams_instances = sum(tags_counts.values())
        tags_log_probabilities = {tag: math.log(occurrences / num_of_unigrams_instances) for tag, occurrences in tags_counts.items()}
        seg_pos_counts = Counter(seg_pos for sentence in seg_pos_tuple_list for seg_pos in sentence)
        if smoothing:
            for seg_pos in product(all_segments, tags_counts.keys()):
                seg_pos_counts[seg_pos] += 0

        seg_pos_log_probabilities = {seg_pos: math.log((occurrences + delta)/ (num_of_unigrams_instances + v)) for seg_pos, occurrences in seg_pos_counts.items()}

        emission_log_probabilities = {(seg, pos):  log_probability - tags_log_probabilities[pos] for (seg, pos), log_probability in seg_pos_log_probabilities.items()}

        sentences_pos = [[pos for _, pos in sentence] for sentence in seg_pos_tuple_list]
        transitions_counts = Counter(chain.from_iterable(zip(['<S>'] + sentence, sentence) for sentence in sentences_pos))

        v = len(tags_counts) * delta
        if smoothing:
            for seg_pos in product(tags_counts.keys(), tags_counts.keys()):
                seg_pos_counts[seg_pos] += 0
        transitions_probabilities = {(src, dst): math.log((occurrences + delta) / (tags_counts[src] + v)) for (src, dst), occurrences in transitions_counts.items()}

        lex_file = join(EXPERIMENTS_DIR, 'hmm_{}_{}_smoothing.lex'.format(self.__order, 'with' if smoothing else 'without'))
        grams_file = join(EXPERIMENTS_DIR, 'hmm_{}_{}_smoothing.gram'.format(self.__order, 'with' if smoothing else 'without'))
        with open(lex_file, 'w') as f:
            writer = csv.writer(f, delimiter='\t')
            for segment, seg_pos_log_proba in groupby(sorted(emission_log_probabilities.items()), key=lambda item: item[0][0]):
                writer.writerow([segment] + list(chain.from_iterable((pos, log_probability) for (_, pos), log_probability in seg_pos_log_proba)))
                writer.writerow([UNKNOWN, 'NNP', 0])

        with open(grams_file, 'w') as f:
            f.write('\\data\\\n')
            f.write('ngram 1 = {}\n'.format(num_of_unigrams_instances))
            f.write('ngram 2 = {}\n'.format(sum(transitions_counts.values())))
            f.write('\n')
            f.write('\\1-grams\\\n')
            writer = csv.writer(f, delimiter='\t')
            writer.writerows(map(reversed, tags_log_probabilities.items()))
            f.write('\n')
            f.write('\\2-grams\\\n')
            for (src_pos, dst_pos), log_proba in transitions_probabilities.items():
                writer.writerow([log_proba, src_pos, dst_pos])

    def decode(self, test_file, lex_file, gram_file):
        emission_probabilities = self.__load_emission_probabilities(lex_file)
        transition_probabilities = self.__load_transitions_probabilities(gram_file)

        all_possible_pos = list(set(k for probabilities in emission_probabilities.values() for k in probabilities))

        label = splitext(basename(lex_file))[0]
        with open(join(EXPERIMENTS_DIR, label + '_' + splitext(basename(test_file))[0] + '.tagged'), 'wt') as f:
            writer = csv.writer(f, delimiter='\t')
            for sentence in tqdm(DocumentsReader.read(test_file)):
                segments_tags = self.__predict_tags(sentence, all_possible_pos,
                                                    emission_probabilities,
                                                    transition_probabilities)

                writer.writerows(segments_tags)
                f.write('\n')

    @staticmethod
    def __predict_tags(sentence, all_possible_pos, emission_probabilities, transition_probabilities):
        recent_pos = '<S>'
        mat = [[None] * (len(sentence)) for _ in range(len(all_possible_pos))]
        first_segment = sentence[0].segment if sentence[0].segment in emission_probabilities else UNKNOWN
        for i, pos in enumerate(all_possible_pos):
            mat[i][0] = transition_probabilities[(recent_pos, pos)]
            mat[i][0] += emission_probabilities[first_segment].get(pos, -float('inf'))

        for j in range(1, len(sentence)):
            segment = sentence[j].segment if sentence[j].segment in emission_probabilities else UNKNOWN
            for i, pos in enumerate(all_possible_pos):
                mat[i][j] = emission_probabilities[segment].get(pos, -float('inf'))
                max_transition_probability = float('-inf')
                for k, prev_pos in enumerate(all_possible_pos):
                    transition_probability = mat[k][j - 1] + transition_probabilities[(prev_pos, pos)]
                    max_transition_probability = max(max_transition_probability, transition_probability)
                mat[i][j] += max_transition_probability

        pos_positions = [max(range(len(all_possible_pos)), key=lambda i: mat[i][j])
                         for j in range(len(sentence))]
        segments_tags = [(part.segment, all_possible_pos[pos_position])
                         for part, pos_position in
                         zip(sentence, pos_positions)]

        return segments_tags

    @staticmethod
    def __load_transitions_probabilities(gram_file):
        gram_order = 2

        transition_probabilities = defaultdict(lambda: -float('inf'))
        with open(gram_file, 'rt') as f:
            list(takewhile(lambda line: line != '\\{}-grams\\\n'.format(gram_order), f))

            reader = csv.reader(f, delimiter='\t')
            for row in reader:
                assert len(row) == 3
                transition_probabilities[tuple(row[1:])] = float(row[0])
        return transition_probabilities

    @staticmethod
    def __load_emission_probabilities(lex_file):
        emission_probabilities = defaultdict(dict)

        with open(lex_file, 'r') as f:
            reader = csv.reader(f, delimiter='\t')
            for row in reader:
                segment = row[0]
                for pos, prob in zip(row[1::2], row[2::2]):
                    emission_probabilities[segment][pos] = float(prob)
        return emission_probabilities
