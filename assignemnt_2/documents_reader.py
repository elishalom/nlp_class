import csv
from typing import Generator, Tuple, List


class DocumentsReader(object):
    @staticmethod
    def read(docs_file: str):
        with open(docs_file, 'rt') as f:
            reader = csv.reader(f, delimiter='\t')
            sentence = []
            for row in reader:
                if len(row) == 0:
                    yield sentence
                    sentence = []
                else:
                    sentence.append(tuple(row))
