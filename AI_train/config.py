from pathlib import Path

class ModelConfig:
    SEQUENCE_LENGTH = 60
    FEATURE_DIM = 225
    NUM_CLASSES = 300
    BATCH_SIZE = 32
    EPOCHS = 100
    LEARNING_RATE = 1e-3

    DATA_ROOT = Path("./data")
    MODEL_SAVA_PATH = Path("./models")