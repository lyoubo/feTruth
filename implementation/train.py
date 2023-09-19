import os

import numpy as np
from gensim.models import word2vec
from keras.layers import Conv1D
from keras.layers import Dense, Flatten
from keras.layers import merge
from keras.models import Model
from keras.models import Sequential
from keras.utils.np_utils import to_categorical
from keras_preprocessing.sequence import pad_sequences
from keras_preprocessing.text import Tokenizer

from implementation.preprocess import preprocess_training_data

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
np.random.seed(1337)
MAX_SEQUENCE_LENGTH = 15
EMBEDDING_DIM = 200
DATA_PATH = 'training_data/'


def train():
    texts = []
    distances = []
    cbmc_distances = []
    mcmc_distances = []
    labels = []
    with open(DATA_PATH + 'real_world/train_name.txt', 'r', encoding='utf-8') as f:
        for line in f.readlines():
            texts.append(line)
    with open(DATA_PATH + 'real_world/train_label.txt', 'r', encoding='utf-8') as f:
        for line in f.readlines():
            labels.append(line)
    with open(DATA_PATH + 'real_world/train_dist.txt', 'r', encoding='utf-8') as f:
        for line in f.readlines():
            values = line.split()
            distances.append(values)
    with open(DATA_PATH + 'real_world/train_cbmc.txt', 'r', encoding='utf-8') as f:
        for line in f.readlines():
            values = line.split()
            cbmc_distances.append(values)
    with open(DATA_PATH + 'real_world/train_mcmc.txt', 'r', encoding='utf-8') as f:
        for line in f.readlines():
            values = line.split()
            mcmc_distances.append(values)

    embedding_model = word2vec.Word2Vec.load('../word2vec/new_model.bin')
    tokenizer = Tokenizer(num_words=None)
    tokenizer.fit_on_texts(texts)
    sequences = tokenizer.texts_to_sequences(texts)
    word_index = tokenizer.word_index
    nb_words = len(word_index)
    embedding_matrix = np.zeros((nb_words + 1, EMBEDDING_DIM))
    for word, i in word_index.items():
        try:
            embedding_vector = embedding_model.wv[word]
        except KeyError:
            embedding_vector = [0 for i in range(EMBEDDING_DIM)]
        if embedding_vector is not None:
            embedding_matrix[i] = embedding_vector
    data = pad_sequences(sequences, maxlen=MAX_SEQUENCE_LENGTH)
    name_vector = np.zeros((len(data), MAX_SEQUENCE_LENGTH, EMBEDDING_DIM))
    for i in range(len(data)):
        name_vector[i] = [embedding_matrix[data[i][j]] for j in range(MAX_SEQUENCE_LENGTH)]

    x_train = []
    distances = np.asfarray(distances)
    distances = np.expand_dims(distances, axis=2)
    x_train.append(name_vector)
    x_train.append(np.array(distances))
    cbmc_distances = np.asfarray(cbmc_distances)
    cbmc_distances = np.expand_dims(cbmc_distances, axis=2)
    x_train.append(np.array(cbmc_distances))
    mcmc_distances = np.asfarray(mcmc_distances)
    mcmc_distances = np.expand_dims(mcmc_distances, axis=2)
    x_train.append(np.array(mcmc_distances))
    labels = to_categorical(np.asarray(labels))
    y_train = np.array(labels)

    print('Training model...')

    model_name = Sequential()
    model_name.add(Conv1D(128, 1, input_shape=(MAX_SEQUENCE_LENGTH, EMBEDDING_DIM), padding='same', activation='tanh'))
    model_name.add(Conv1D(128, 1, activation='tanh'))
    model_name.add(Conv1D(128, 1, activation='tanh'))
    model_name.add(Flatten())

    model_dist = Sequential()
    model_dist.add(Conv1D(128, 1, input_shape=(2, 1), padding='same', activation='tanh'))
    model_dist.add(Conv1D(128, 1, activation='tanh'))
    model_dist.add(Conv1D(128, 1, activation='tanh'))
    model_dist.add(Flatten())

    model_cbmc = Sequential()
    model_cbmc.add(Conv1D(128, 1, input_shape=(2, 1), padding='same', activation='tanh'))
    model_cbmc.add(Conv1D(128, 1, activation='tanh'))
    model_cbmc.add(Conv1D(128, 1, activation='tanh'))
    model_cbmc.add(Flatten())

    model_mcmc = Sequential()
    model_mcmc.add(Conv1D(128, 1, input_shape=(2, 1), padding='same', activation='tanh'))
    model_mcmc.add(Conv1D(128, 1, activation='tanh'))
    model_mcmc.add(Conv1D(128, 1, activation='tanh'))
    model_mcmc.add(Flatten())

    output = merge.Concatenate()([model_name.output, model_dist.output, model_cbmc.output, model_mcmc.output])
    output = Dense(128, activation='tanh')(output)
    output = Dense(2, activation='sigmoid')(output)
    input_names = model_name.input
    input_dist = model_dist.input
    input_cbmc = model_cbmc.input
    input_mcmc = model_mcmc.input
    model = Model([input_names, input_dist, input_cbmc, input_mcmc], output)
    model.compile(optimizer='adam', loss='binary_crossentropy', metrics=['accuracy'])
    model.fit(x_train, y_train, epochs=20, shuffle=True, verbose=1)
    model.save('feTruth.h5')


if __name__ == '__main__':
    if not os.path.exists('training_data/real_world'):
        preprocess_training_data()
    train()
