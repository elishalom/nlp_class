#!/usr/bin/env python3
import sys

from models.model_loader import ModelLoader

if __name__ == '__main__':
    model_code = sys.argv[1]
    test_file = sys.argv[2]

    model = ModelLoader().load(model_code)
    model.decode(test_file, *sys.argv[3:])
