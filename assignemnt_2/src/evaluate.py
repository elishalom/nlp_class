#!/usr/bin/env python3
import sys

from models.model_loader import ModelLoader

if __name__ == '__main__':
    tagged_file = sys.argv[1]
    gold_file = sys.argv[2]
    model_code = sys.argv[3]
    smoothing = sys.argv[4]

    model = ModelLoader().load(model_code)

    model.evaluate(model, tagged_file, gold_file, smoothing)
