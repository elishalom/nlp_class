#!/usr/bin/env python3

import sys
from os.path import basename, splitext

from assignemnt_2.baseline_trainer import BaselineTrainer
from assignemnt_2.default_tagged_data_load import DefaultTaggedDataLoader
from assignemnt_2.hmm import HMM

if __name__ == '__main__':
    model_name = sys.argv[1]
    train_file = sys.argv[2]

    if model_name == 'baseline':
        model = BaselineTrainer()
        model.train(DefaultTaggedDataLoader.load(train_file))
        model.persist(splitext(basename(train_file))[0] + '.param')

    if model_name == 'hmm':
        model = HMM()
        model.train(train_file)
        # baseline.persist(splitext(basename(train_file))[0] + '.param')
