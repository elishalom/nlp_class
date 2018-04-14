#!/usr/bin/env python3

import sys
from os.path import basename, splitext

from assignemnt_2.baseline_trainer import BaselineTrainer
from assignemnt_2.default_tagged_data_load import DefaultTaggedDataLoader

if __name__ == '__main__':
    model = sys.argv[1]
    train_file = sys.argv[2]

    if model == 'baseline':
        baseline = BaselineTrainer()
        baseline.train(DefaultTaggedDataLoader.load(train_file))
        baseline.persist(splitext(basename(train_file))[0] + '.param')
