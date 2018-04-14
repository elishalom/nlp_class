import pandas as pd


class BaselineTrainer(object):
    def train(self, tagged_corpus: pd.DataFrame) -> pd.DataFrame:
        occurrences_per_tag = tagged_corpus.groupby(['segment', 'type']).size().to_frame('occurrences')
        max_tag = occurrences_per_tag.groupby('segment')['occurrences'].max().to_frame('max_occurrences')
        occurrences_per_tag = occurrences_per_tag.join(max_tag)
        occurrences_per_tag = occurrences_per_tag.loc[occurrences_per_tag['occurrences'] == occurrences_per_tag['max_occurrences']]
        occurrences_per_tag = occurrences_per_tag.loc[~occurrences_per_tag.index.duplicated()]
        occurrences_per_tag = occurrences_per_tag[[]].reset_index('type')
        return occurrences_per_tag
