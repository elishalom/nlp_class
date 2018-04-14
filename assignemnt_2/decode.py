#!/usr/bin/env python3
import sys

from assignemnt_2.baseline_trainer import BaselineTrainer

if __name__ == '__main__':
    model = sys.argv[1]
    test_file = sys.argv[2]
    param_file = sys.argv[3]

    if model == 'baseline':
        baseline = BaselineTrainer()
        baseline.load(param_file)
        baseline.tag(test_file)
