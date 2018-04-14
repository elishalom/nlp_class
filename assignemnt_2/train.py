#!/usr/bin/env python3

import sys

from assignemnt_2.baseline_trainer import BaselineTrainer
from assignemnt_2.hmm import HMM

if __name__ == '__main__':
    model_name = sys.argv[1]
    train_file = sys.argv[2]

    if model_name == 'baseline':
        model = BaselineTrainer()
        model.train(train_file)

    if model_name == 'hmm':
        model = HMM()
        model.train(train_file)
