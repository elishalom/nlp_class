from assignemnt_2.tagged_data_normalizer import TaggedDataNormalizer
from assignemnt_2.tagged_data_reader import TaggedDataLoader


class DefaultTaggedDataLoader(object):
    @staticmethod
    def load(source_file):
        return TaggedDataLoader(TaggedDataNormalizer()).load(source_file)