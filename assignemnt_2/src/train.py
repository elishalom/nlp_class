import sys

from models.model_loader import ModelLoader

if __name__ == '__main__':
    model_code = sys.argv[1]
    train_file = sys.argv[2]
    smoothing = True if len(sys.argv) > 3 and sys.argv[3] == 'y' else False

    model = ModelLoader().load(model_code)

    model.train(train_file, smoothing)
