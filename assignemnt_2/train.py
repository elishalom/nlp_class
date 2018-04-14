#!/usr/bin/env python3
import sys

from assignemnt_2.baseline_trainer import BaselineTrainer
from assignemnt_2.default_tagged_data_load import DefaultTaggedDataLoader

if __name__ == '__main__':
    model = sys.argv[1]
    tags_file = sys.argv[2]

    if model == 'baseline':
        BaselineTrainer().train(DefaultTaggedDataLoader.load(tags_file))
