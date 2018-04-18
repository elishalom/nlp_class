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
        # setting parameters files
        lex_file = splitext(basename(train_file))[0] + '.lex'
        gram_file = splitext(basename(train_file))[0] + '.gram'

        # representing emmision and transition probabilities
        em_probs = defaultdict(lambda: defaultdict(float))
        trans_probs = defaultdict(lambda: defaultdict(float))

        # reading corpus to list of segment-tags (by sentences
        seg_pos_tuple_list = DocumentsReader.read(train_file)

        # POS tag probabilities
        pos_prob = defaultdict(int)

        # Viterbi initial vector
        initial_vector = defaultdict(float)

        # iterating over the seg-tag tuples list (by sentence)
        for sentence in seg_pos_tuple_list:
            # adding a <s> tag for beginning of sentence (s_0 in viterbi)
            last_pos = '<S>'
            # setting the initial vector for the first pos-tag in each sentence #TODO: wrong!!! we're not in viterbi yet
            initial_vector[sentence[0][1]] += 1

            # iterating over all the seg-tag tuples in the current sentence
            for seg, pos in sentence:
                pos_prob[pos] += 1 # adding tag-unigram to count (pos-unigram)
                em_probs[seg][pos] += 1 # adding the seg-tag to emmision count (to be later divided by appropriate pos-unigram in Viterbi)
                if last_pos is not None: # adding pos-pos to transition count (to be to be later divided by appropriate pos-unigram in Viterbi)
                    trans_probs[last_pos][pos] += 1
                last_pos = pos

            # add default unkown words to NNP
            if not(smoothing):
                em_probs["UNK"]["NNP"] = 1

        pos_counts = sum(pos_prob.values()) # total amount of counts TODO: maybe take from the first question?
        delta_pos = 1 if smoothing else 0 # add_delta smoothing TODO: maybe optimize as funtion of distribution? (add-1 distorts results)
        pos_prob = {k: log((v + delta_pos) / (pos_counts + delta_pos * pos_counts)) for k, v in pos_prob.items()} # TODO: where is it used? applies to transitions too (no)?

        # calculating log propabilities for transitions
        for pos in trans_probs.keys():
            adjacent_pos_counts = sum(trans_probs[pos].values())
            for adjacent_pos in trans_probs[pos]:
                trans_probs[pos][adjacent_pos] = log(trans_probs[pos][adjacent_pos] / adjacent_pos_counts)

        # calculating log propabilities for emmisions # TODO: not really emissions ... intersections ...
        for seg in em_probs.keys():
            segment_tags = sum(em_probs[seg].values())
            for pos in em_probs[seg]:
                em_probs[seg][pos] = log(em_probs[seg][pos] / segment_tags)

        # writing lexical parameter file
        with open(lex_file, 'w') as f:
            writer = csv.writer(f, delimiter='\t')
            for segment, pos_log_proba in em_probs.items():
                writer.writerow([segment] + list(itertools.chain.from_iterable(sorted(pos_log_proba.items()))))

        # writing transition parameter file
        with open(gram_file, 'w') as f:
            f.write('\\data\\\n')
            f.write('ngram 1 = {}\n'.format(len(em_probs)))
            f.write('\n')
            # f.write('ngram 2 = {}\n'.format(len(em_probs))) # TODO : count bi-grams
            # f.write('\n')
            f.write('\\1-grams\\\n')
            writer = csv.writer(f, delimiter='\t')
            writer.writerows(map(reversed, pos_prob.items()))
            f.write('\n')
            f.write('\\2-grams\\\n')
            for pos, transitions in trans_probs.items():
                for adjacent_pos, log_proba in transitions.items():
                    writer.writerow([log_proba, pos, adjacent_pos])

    def decode(self, test_file, lex_file, grams_file):
        # load segment-tag probabilities from lexical parameter file
        emition_probabilities = defaultdict(dict) # TODO: replace all misspellings of emission related
        with open(lex_file, 'r') as f:
            reader = csv.reader(f, delimiter='\t')
            for row in reader:
                segment = row[0]
                for pos, prob in zip(row[1::2], row[2::2]):
                    emition_probabilities[segment][pos] = float(prob)

        all_possible_pos = list(set(itertools.chain.from_iterable(emition_probabilities.values())))

        gram_order = 2 # TODO : move somewhere else (looks like it should be on function call)

        transition_probabilities = {}
        with open(grams_file, 'rt') as f:
            list(takewhile(lambda line: line != '\\{}-grams\\\n'.format(gram_order), f)) # TODO: maybe add newline ignore case to takewhile
            reader = csv.reader(f, delimiter='\t')
            for row in reader:
                transition_probabilities[tuple(row[1:])] = float(row[0])

        # TODO - add somehow to previous loop ... ugly code and inefficient
        pos_log_probs = {} # pos-tag unigram log probabilities
        with open(grams_file, 'rt') as f:
            list(takewhile(lambda line: line != '\\1-grams\\\n',f))  # TODO: maybe add newline ignore case to takewhile
            reader = csv.reader(f, delimiter='\t')
            for row in reader:
                if len(row)==0:
                    break
                pos_log_probs[row[1]]=float(row[0])


        with open(splitext(basename(test_file))[0] + '.tagged', 'wt') as f:
            writer = csv.writer(f, delimiter='\t')
            for sentence in tqdm(DocumentsReader.read(test_file)):
                # set-up Viterbi Matrix
                recent_pos = '<S>'
                mat = [[None] * (len(sentence)) for _ in range(len(all_possible_pos))]
                first_segment = sentence[0][0]
                for i, pos in enumerate(all_possible_pos):
                    mat[i][0] = (transition_probabilities[(recent_pos, pos)]-pos_log_probs[pos]) if (recent_pos, pos) in transition_probabilities else float('-inf') # TODO: changed ... also - why e**? we log again as it is
                    mat[i][0] += (emition_probabilities[first_segment][pos]-pos_log_probs[pos]) if first_segment in emition_probabilities and pos in emition_probabilities[first_segment] else float('-inf')
                    # mat[i][0] = math.log(mat[i][0]) if mat[i][0] > 0 else float('-inf')

                for j in range(1, len(sentence)):
                    segment = sentence[j][0]
                for i, pos in enumerate(all_possible_pos):
                    if not (segment in (emition_probabilities)):
                        segment = "UNK"
                    mat[i][j] = (emition_probabilities[segment][pos]-pos_log_probs[pos]) if pos in emition_probabilities[segment] else float('-inf') # TODO: why still dependant on first segment? not previous?
                    max_transition_probability = float('-inf')
                    for k, prev_pos in enumerate(all_possible_pos):
                        transition_probability = ((mat[k][j-1]) + (transition_probabilities[(prev_pos, pos)]-pos_log_probs[pos])) if (prev_pos, pos) in transition_probabilities else float('-inf')
                        max_transition_probability = max(max_transition_probability, transition_probability)
                    mat[i][j] += max_transition_probability
                    # mat[i][j] = math.log(mat[i][j])

                pos_positions = [max(range(len(all_possible_pos)), key=lambda i: mat[i][j]) for j in range(len(sentence))]

                segments_tags = [(part[0], all_possible_pos[pos_position]) for part, pos_position in
                                 zip(sentence, pos_positions)]

                writer.writerows(segments_tags)
                f.write('\n')
