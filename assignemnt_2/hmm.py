import csv
import itertools
import math
from collections import defaultdict, Counter
from itertools import takewhile, chain
from operator import itemgetter
from os.path import basename, splitext
from typing import Dict, Tuple, List

from tqdm import tqdm

from assignemnt_2.documents_reader import DocumentsReader
from assignemnt_2.model_base import ModelBase


class HMM(ModelBase):
    def __init__(self, order: int=1) -> None:
        super().__init__()
        self.order = order

    def train(self, train_file: str, smoothing: bool= True) -> None:
        lex_file = splitext(basename(train_file))[0] + '.lex'
        gram_file = splitext(basename(train_file))[0] + '.gram'

        seg_pos_tuple_list = list(DocumentsReader.read(train_file))

        all_possible_pos = sorted(set(part.pos for sentence in seg_pos_tuple_list for part in sentence))

        pairs_count = Counter(chain.from_iterable(seg_pos_tuple_list))
        emission_probabilities = self.__to_log_probabilities(pairs_count, smoothing, all_possible_pos)

        sentences_pos = [[pos for _, pos in sentence] for sentence in seg_pos_tuple_list]
        transitions_counts = Counter(chain.from_iterable(zip(['<S>'] + sentence, sentence) for sentence in sentences_pos))
        transitions_probabilities = self.__to_log_probabilities(transitions_counts, smoothing, all_possible_pos)

        with open(lex_file, 'w') as f:
            writer = csv.writer(f, delimiter='\t')
            for segment, pos_log_proba in emission_probabilities.items():
                writer.writerow([segment] + list(itertools.chain.from_iterable(sorted(pos_log_proba.items()))))

        with open(gram_file, 'w') as f:
            f.write('\\data\\\n')
            f.write('ngram 1 = {}\n'.format(len(emission_probabilities)))
            f.write('\n')
            f.write('\\1-grams\\\n')
            writer = csv.writer(f, delimiter='\t')
            # writer.writerows(map(reversed, pos_prob.items()))
            f.write('\n')
            f.write('\\2-grams\\\n')
            for src_pos, transitions in transitions_probabilities.items():
                for dst_pos, log_proba in transitions.items():
                    writer.writerow([log_proba, src_pos, dst_pos])

    @staticmethod
    def __to_log_probabilities(pairs_count: Dict[Tuple[str, str], int],
                               smoothing: bool=False,
                               fillers: List[str]=None):
        segment_to_pos_occurrences: Dict[str, Dict[str, int]] = defaultdict(lambda: defaultdict(int))
        for (segment, pos), count in pairs_count.items():
            segment_to_pos_occurrences[segment][pos] = count
        if smoothing and fillers:
            for segment in segment_to_pos_occurrences:
                for pos in fillers:
                    segment_to_pos_occurrences[segment][pos] += 1

        segment_occurrences = {segment: sum(pas_to_occurrences.values())
                               for segment, pas_to_occurrences
                               in segment_to_pos_occurrences.items()}
        emission_probabilities = {
            segment: {pos: math.log(count / segment_occurrences[segment]) for pos, count in pas_to_occurrences.items()} for
            segment, pas_to_occurrences in
            segment_to_pos_occurrences.items()}
        return emission_probabilities

    def decode(self, test_file, lex_file, grams_file):
        emission_probabilities = self.__load_emission_probabilities(lex_file)
        transition_probabilities = self.__load_transitions_probabilities(grams_file)

        all_possible_pos = sorted(set(map(itemgetter(1), emission_probabilities)), key=lambda p: (p != 'NNP', p))

        with open(splitext(basename(test_file))[0] + '.tagged', 'wt') as f:
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
        first_segment = sentence[0].segment
        for i, pos in enumerate(all_possible_pos):
            mat[i][0] = transition_probabilities[(recent_pos, pos)]
            mat[i][0] += emission_probabilities[(first_segment, pos)]

        for j in range(1, len(sentence)):
            segment = sentence[j].segment
            for i, pos in enumerate(all_possible_pos):
                mat[i][j] = emission_probabilities[(segment, pos)]
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
    def __load_transitions_probabilities(grams_file):
        gram_order = 2

        # The fallback value is not expected to be hit if smoothing was used in train
        transition_probabilities = defaultdict(lambda: -float('inf'))
        with open(grams_file, 'rt') as f:
            list(takewhile(lambda line: line != '\\{}-grams\\\n'.format(gram_order), f))

            reader = csv.reader(f, delimiter='\t')
            for row in reader:
                assert len(row) == 3
                transition_probabilities[tuple(row[1:])] = float(row[0])
        return transition_probabilities

    @staticmethod
    def __load_emission_probabilities(lex_file):
        # This 1/1000 is a fallback value for seg-pos we haven't seen yet.
        # This needs to be replaced with calculated smoothing.
        emission_probabilities = defaultdict(lambda: math.log(1/1000))

        with open(lex_file, 'r') as f:
            reader = csv.reader(f, delimiter='\t')
            for row in reader:
                segment = row[0]
                for pos, prob in zip(row[1::2], row[2::2]):
                    emission_probabilities[(segment, pos)] = float(prob)
        return emission_probabilities
