class CorpusStats(object):
    def __init__(self,
                 num_of_unigrams_instances: int,
                 num_of_unigrams: int,
                 num_of_segment_types_instances: int,
                 num_of_segment_types:int ) -> None:
        super().__init__()
        self.num_of_unigrams_instances = num_of_unigrams_instances
        self.num_of_unigrams = num_of_unigrams
        self.num_of_segment_types_instances = num_of_segment_types_instances
        self.num_of_segment_types = num_of_segment_types
        self.disambiguation = self.num_of_segment_types_instances / self.num_of_unigrams

    def __repr__(self) -> str:
        return 'Unigrams={}, UnigramsInstances={}, Tags={}, TagsInstances={}, rate={}'.format(self.num_of_unigrams, self.num_of_unigrams_instances, self.num_of_segment_types, self.num_of_segment_types_instances, self.disambiguation)
