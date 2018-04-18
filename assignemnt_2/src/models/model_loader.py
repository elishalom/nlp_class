from typing import Union

from models.baseline_trainer import BaselineTrainer
from models.hmm import HMM
from models.model_base import ModelBase


class ModelLoader(object):
    def load(self, model_code: Union[int, str]) -> ModelBase:
        model_code = int(model_code)
        if model_code == 1:
            return BaselineTrainer()

        return HMM(model_code)