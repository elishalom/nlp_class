#!/usr/bin/env python3
import sys

from assignemnt_2.baseline_trainer import BaselineTrainer
from assignemnt_2.hmm import HMM

if __name__ == '__main__':
    mode_name = sys.argv[1]
    test_file = sys.argv[2]
    lex_file = sys.argv[3]
    gram_file = sys.argv[4]

    if mode_name == 'baseline':
        model = BaselineTrainer('segment_to_tag.tsv')
        model.decode(test_file)

    if mode_name == 'hmm':
        model = HMM()
        model.decode(test_file, lex_file, gram_file)
