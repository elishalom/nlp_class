import pandas as pd

from assignemnt_2.tagged_data_normalizer import TaggedDataNormalizer


class TaggedDataLoader(object):
    def __init__(self, normalizer: TaggedDataNormalizer) -> None:
        super().__init__()
        self._normalizer = normalizer

    def load(self, file_name: str) -> pd.DataFrame:
        with open(file_name, 'rt') as f:
            normalized_rows = self._normalizer.normalize(f)
            df = pd.DataFrame(normalized_rows, columns=['document_id', 'position', 'segment', 'tag'])
        return df
