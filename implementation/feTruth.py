import json
import os
import sys
import subprocess
import numpy as np
from gensim.models import word2vec
from keras.models import load_model
from keras_preprocessing.sequence import pad_sequences
from keras_preprocessing.text import Tokenizer

from implementation.preprocess import preprocess_testing_data

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
np.random.seed(1337)
MAX_SEQUENCE_LENGTH = 15
EMBEDDING_DIM = 200
DATA_PATH = 'testing_data/'
THRESHOLD = 0.95


def preprocess_file(filename):
    f = open(filename, 'r')
    data = []
    count = 1
    tmp = []
    for line in f.readlines():
        line = line.strip()
        lst = line.split(' ')
        index = int(lst[0])
        if index == count:
            tmp.append(lst[1:])
        else:
            data.append(tmp)
            tmp = [lst[1:]]
            count += 1
    if len(tmp) != 0:
        data.append(tmp)
    f.close()
    return data


def process_label(filename):
    f = open(filename, 'r')
    data = []
    count = 1
    index = 0
    tmp = None
    for line in f.readlines():
        line = line.strip()
        lst = line.split(' ')
        index += 1
        if index == count:
            tmp = lst[1]
        else:
            data.append(tmp)
            tmp = lst[1]
            count += 1
    data.append(tmp)
    f.close()
    return data


def evaluate(project):
    model = load_model('feTruth.h5')
    embedding_model = word2vec.Word2Vec.load('../word2vec/new_model.bin')
    total_reported = 0
    output = ""
    project_path = DATA_PATH + project
    texts = preprocess_file(project_path + '/test_name.txt')
    distances = preprocess_file(project_path + '/test_dist.txt')
    cbmc_distances = preprocess_file(project_path + '/test_cbmc.txt')
    mcmc_distances = preprocess_file(project_path + '/test_mcmc.txt')
    reported = 0
    reports = []
    accepted_targets = []

    print('Detecting feature envy methods...')

    for ith in range(len(texts)):
        target_class_names = []
        test_name = texts[ith]
        for i in range(len(test_name)):
            target_class_names.append(test_name[i][10:])
        raw_name = []
        for name in test_name:
            raw_name.append(' '.join(name))

        tokenizer = Tokenizer(num_words=None)
        tokenizer.fit_on_texts(raw_name)
        test_sequences = tokenizer.texts_to_sequences(raw_name)
        word_index = tokenizer.word_index
        nb_words = len(word_index)
        embedding_matrix = np.zeros((nb_words + 1, EMBEDDING_DIM))
        for word, i in word_index.items():
            try:
                embedding_vector = embedding_model.wv[word]
            except Exception:
                embedding_vector = [0 for i in range(EMBEDDING_DIM)]
            if embedding_vector is not None:
                embedding_matrix[i] = embedding_vector
        test_data = pad_sequences(test_sequences, maxlen=MAX_SEQUENCE_LENGTH)
        name_vector = np.zeros((len(test_data), MAX_SEQUENCE_LENGTH, EMBEDDING_DIM))
        for i in range(len(test_data)):
            name_vector[i] = [embedding_matrix[test_data[i][j]] for j in range(MAX_SEQUENCE_LENGTH)]

        x_test = []
        x_test.append(name_vector)
        test_dist = np.asfarray(distances[ith])
        test_dist = np.expand_dims(test_dist, axis=2)
        x_test.append(np.array(test_dist))
        test_cbmc = np.asfarray(cbmc_distances[ith])
        test_cbmc = np.expand_dims(test_cbmc, axis=2)
        x_test.append(np.array(test_cbmc))
        test_mcmc = np.asfarray(mcmc_distances[ith])
        test_mcmc = np.expand_dims(test_mcmc, axis=2)
        x_test.append(np.array(test_mcmc))

        detector = model.predict(x_test)
        # result = [np.argmax(detector[i]) for i in range(len(detector))]
        result = [1 if detector[i][1] >= THRESHOLD else 0 for i in range(len(detector))]

        tpreds = []
        for i in range(len(result)):
            if result[i] == 1:
                tpreds.append(1)
            else:
                tpreds.append(0)
        preds = np.array(tpreds)
        num_one = 0
        for i in range(len(preds)):
            if preds[i] == 1:
                num_one += 1

        if num_one != 0:
            reported += 1
            total_reported += 1
            reports.append(ith + 1)
            preds_double = []
            for i in range(len(detector)):
                preds_double.append(detector[i][1])
            max_value = max(preds_double)
            for i in range(len(preds_double)):
                if preds_double[i] == max_value:
                    accepted_targets.append(i)
                    break

    file = DATA_PATH + project + '.json'
    with open(file, encoding='utf-8') as f:
        print('Refactoring Type\tSource Method\tTarget Class')
        records = json.load(f)['RECORDS']
        for record in records:
            for i in range(len(reports)):
                if record['id'] == reports[i]:
                    method_signature = record['method_signature']
                    source_class_name = record['source_class_name']
                    target_class_name = record['target_class_list'].split(', ')[accepted_targets[i]]
                    print('Move Method\t%s::%s \t %s' %
                          (source_class_name, method_signature, target_class_name))
                    output += ('Move Method\t%s::%s \t %s' %
                               (source_class_name, method_signature, target_class_name))
                    output += "\n"
                    break
        print()
        output += "\n"
    if not os.path.exists('output'):
        os.mkdir("output")
    with open('output/' + project + '.txt', mode='w', encoding='utf-8') as f:
        f.write('Refactoring Type\tSource Method\tTarget Class\n')
        f.write(output[:-2])
    print("The feature envy methods are appended to the output folder.")


def print_tips():
    print("-h\t\t\t\t\t\t\t\t\t\t\tShow options")
    print("-a <project-folder>\t\t\t\tDetect all feature envy methods for <project-folder>")


def check_command():
    if not os.path.exists(sys.argv[2]):
        print("Please type the correct folder.")
        print("Type `python feTruth.py -h` to show usage.")
        exit()


def main():
    if len(sys.argv) < 2:
        print("Type `python feTruth.py -h` to show usage.")
        exit()
    option = sys.argv[1]
    if option.lower() == "-h" or option.lower() == "--h" or option.lower() == "-help" or option.lower() == "--help":
        print_tips()
        exit()
    elif option == '-a':
        check_command()
        project = os.path.basename(sys.argv[2])
        jar_path = "testingDataGeneration.jar"
        java_command = ["java", "-jar", jar_path, sys.argv[2]]
        process = subprocess.Popen(java_command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        process.communicate()
        preprocess_testing_data(project)
        evaluate(project)
    else:
        print("Type `python feTruth.py -h` to show usage.")
        exit()


if __name__ == '__main__':
    main()
