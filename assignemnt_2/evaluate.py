#!/usr/bin/env python3
import sys

from assignemnt_2.baseline_trainer import BaselineTrainer

if __name__ == '__main__':
    tagged_file = sys.argv[1]
    gold_file = sys.argv[2]
    model = sys.argv[3]
    smoothing = sys.argv[4]

    if model == 'baseline':
        BaselineTrainer().evaluate(tagged_file, gold_file)
