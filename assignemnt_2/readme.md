
# NLP Assignment Execution notes

## Models
There are two training model:
1. Baseline
2. HMM

## Train

`python train.py MODEL_CODE TRAIN_DATA_FILE SMOOTHING_FLAG`

### Baseline

`python train.py 1 ../exps/heb-pos.train`

### HMM

`python train.py 2 ../exps/heb-pos.train y`
`python train.py 2 ../exps/heb-pos.train n`


## DECODE

`python decode.py MODEL_CODE TEST_FILE *EXTRA_PARAMS`

### Baseline

`python train.py 1 ../exps/heb-pos.test`

### HMM

`python decode.py 2 2 ../exps/heb-pos.test ../exps/hmm_2_with_smoothing.lex ../exps/hmm_2_with_smoothing.gram`

## Evaluate

`python evaluate.py TAGGED_FILE GOLD_FILE MODEL_CODE SMOOTHING`

### Baseline

`python evaluate.py ../exps/hmm_2_with_smoothing_heb-pos.tagged ../exps/heb-pos.gold 1 n`

### HMM

`python evaluate.py ../exps/hmm_2_with_smoothing_heb-pos.tagged ../exps/heb-pos.gold 2 y`
