import csv
from typing import Iterable


class TaggedDataNormalizer(object):
    @staticmethod
    def normalize(f: Iterable):
        reader = csv.reader(f, delimiter='\t')
        document_id = 0
        pos = 0
        for row in reader:
            assert len(row) in (0, 2)
            if len(row) == 0:
                document_id += 1
                pos = 0
            else:
                yield [document_id, pos] + row
                pos += 1
