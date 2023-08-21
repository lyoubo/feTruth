import json
import os

MAX_NUM_WORD = 5
TRAINING_DATA_PATH = 'training_data/'
TESTING_DATA_PATH = 'testing_data/'
TESTING_PROJECTS = ['cli', 'compress', 'csv', 'jsoup', 'time']


def participle(name):
    for ch in name:
        if ch.isupper():
            name = name.replace(ch, ' ' + ch.lower(), 1)
        else:
            if ch == '_' or ch == '$':
                name = name.replace(ch, ' ', 1)
            else:
                if ch.isdigit():
                    name = name.replace(ch, ' ' + ch)
    name1 = name.strip().split(' ')
    sentence = []
    for word in name1:
        if word != '':
            sentence.append(word)
    return sentence


def preprocessing_names(sentence):
    sentence = participle(sentence)
    num = len(sentence)
    sentence1 = ''
    if num < MAX_NUM_WORD:
        for i in range(MAX_NUM_WORD - num):
            sentence1 += '*' + ' '
        sentence1 = sentence1.strip().split(' ')
        sentence1 += sentence
    if num > MAX_NUM_WORD:
        num = MAX_NUM_WORD
        num1 = MAX_NUM_WORD
        sentence1 = ''
        for word in sentence:
            if num1 == MAX_NUM_WORD:
                sentence1 += word
            else:
                sentence1 += ' ' + word
            num1 = num1 - 1
            if num1 == 0:
                break
        sentence = sentence1.strip().split(' ')
    if num == MAX_NUM_WORD:
        sentence1 = sentence
    return sentence1


def process_training(data, filtered):
    features = []
    ground_truth = []
    with open(ROOT_PATH + 'implementation/data-processing/classifier/decision_predict.txt', encoding='utf-8') as f:
        for line in f:
            refactor_id = int(line.strip().split(',')[0])
            label = int(line.strip().split(',')[1])
            if label == 1:
                ground_truth.append(refactor_id)
    for row in data:
        if filtered and row['refactor_id'] not in ground_truth:
            continue
        source_class_name = row['source_class_name'].split('.')[-1]
        method_name = row['method_name']
        source_dist = row['source_dist']
        source_cbmc = row['source_cbmc']
        source_mcmc = row['source_mcmc']
        target_class_name = row['target_class_name'].split('.')[-1]
        target_dist = row['target_dist']
        target_cbmc = row['target_cbmc']
        target_mcmc = row['target_mcmc']

        # processing words
        method_name = preprocessing_names(method_name)
        source_class_name = preprocessing_names(source_class_name)
        target_class_name = preprocessing_names(target_class_name)
        features.append({
            'source_class_name': source_class_name,
            'method_name': method_name,
            'source_dist': source_dist,
            'source_cbmc': source_cbmc,
            'source_mcmc': source_mcmc,
            'target_class_name': target_class_name,
            'target_dist': target_dist,
            'target_cbmc': target_cbmc,
            'target_mcmc': target_mcmc
        })
    return features


def process_testing(file):
    data = []
    with open(file, encoding='utf-8') as f:
        projects = json.load(f)
        for project in projects:
            features = []
            name = project['name']
            records = project['records']
            for record in records:
                source_class_name = record['source_class_name'].split('.')[-1]
                method_name = record['method_name']
                method_signature = record['method_signature']
                source_dist = record['source_dist']
                source_cbmc = record['source_cbmc']
                source_mcmc = record['source_mcmc']
                target_class_list = record['target_class_list'].split(', ')
                for i in range(len(target_class_list)):
                    target_class_list[i] = target_class_list[i].split('.')[-1]
                target_dist_list = record['target_dist_list'].split(', ')
                target_cbmc_list = record['target_cbmc_list'].split(', ')
                target_mcmc_list = record['target_mcmc_list'].split(', ')

                # processing words
                source_class_name = preprocessing_names(source_class_name)
                for i in range(len(target_class_list)):
                    target_class_list[i] = preprocessing_names(target_class_list[i])
                method_name = preprocessing_names(method_name)
                features.append({
                    'source_class_name': source_class_name,
                    'method_name': method_name,
                    'method_signature': method_signature,
                    'source_dist': source_dist,
                    'source_cbmc': source_cbmc,
                    'source_mcmc': source_mcmc,
                    'target_class_list': target_class_list,
                    'target_dist_list': target_dist_list,
                    'target_cbmc_list': target_cbmc_list,
                    'target_mcmc_list': target_mcmc_list
                })
            data.append({'name': name, 'features': features})
        return data


def write_without_filtering_data():
    with open(ROOT_PATH + 'data/raw-data/true_positives.json', encoding='utf-8') as f:
        true_positives = json.load(f)
    tp_features = process_training(true_positives, False)
    with open(ROOT_PATH + 'data/raw-data/true_negatives.json', encoding='utf-8') as f:
        true_negatives = json.load(f)
    tn_features = process_training(true_negatives, False)
    if not os.path.exists(TRAINING_DATA_PATH + 'without_filtering'):
        os.makedirs(TRAINING_DATA_PATH + 'without_filtering')
    unfiltered_name_file = TRAINING_DATA_PATH + 'without_filtering/train_name.txt'
    unfiltered_dist_file = TRAINING_DATA_PATH + 'without_filtering/train_dist.txt'
    unfiltered_cbmc_file = TRAINING_DATA_PATH + 'without_filtering/train_cbmc.txt'
    unfiltered_mcmc_file = TRAINING_DATA_PATH + 'without_filtering/train_mcmc.txt'
    unfiltered_label_file = TRAINING_DATA_PATH + 'without_filtering/train_label.txt'

    unfiltered_name_writer = open(unfiltered_name_file, 'w', encoding='utf-8')
    unfiltered_dist_writer = open(unfiltered_dist_file, 'w', encoding='utf-8')
    unfiltered_cbmc_writer = open(unfiltered_cbmc_file, 'w', encoding='utf-8')
    unfiltered_mcmc_writer = open(unfiltered_mcmc_file, 'w', encoding='utf-8')
    unfiltered_label_writer = open(unfiltered_label_file, 'w', encoding='utf-8')

    for i in range(len(tn_features)):
        tp_feature = tp_features[i]
        tn_feature = tn_features[i]

        label = 1
        unfiltered_label_writer.write(str(label) + '\n')
        method_name = tp_feature['method_name']
        source_class_name = tp_feature['source_class_name']
        target_class_name = tp_feature['target_class_name']
        for word in method_name:
            unfiltered_name_writer.write(word + ' ')
        for word in source_class_name:
            unfiltered_name_writer.write(word + ' ')
        for word in target_class_name:
            unfiltered_name_writer.write(word + ' ')
        unfiltered_name_writer.write('\n')
        source_dist = tp_feature['source_dist']
        target_dist = tp_feature['target_dist']
        unfiltered_dist_writer.write(source_dist + ' ' + target_dist + '\n')
        source_cbmc = tp_feature['source_cbmc']
        target_cbmc = tp_feature['target_cbmc']
        unfiltered_cbmc_writer.write(source_cbmc + ' ' + target_cbmc + '\n')
        source_mcmc = tp_feature['source_mcmc']
        target_mcmc = tp_feature['target_mcmc']
        unfiltered_mcmc_writer.write(source_mcmc + ' ' + target_mcmc + '\n')

        label = 0
        unfiltered_label_writer.write(str(label) + '\n')
        method_name = tn_feature['method_name']
        source_class_name = tn_feature['source_class_name']
        target_class_name = tn_feature['target_class_name']
        for word in method_name:
            unfiltered_name_writer.write(word + ' ')
        for word in source_class_name:
            unfiltered_name_writer.write(word + ' ')
        for word in target_class_name:
            unfiltered_name_writer.write(word + ' ')
        unfiltered_name_writer.write('\n')
        source_dist = tn_feature['source_dist']
        target_dist = tn_feature['target_dist']
        unfiltered_dist_writer.write(source_dist + ' ' + target_dist + '\n')
        source_cbmc = tn_feature['source_cbmc']
        target_cbmc = tn_feature['target_cbmc']
        unfiltered_cbmc_writer.write(source_cbmc + ' ' + target_cbmc + '\n')
        source_mcmc = tn_feature['source_mcmc']
        target_mcmc = tn_feature['target_mcmc']
        unfiltered_mcmc_writer.write(source_mcmc + ' ' + target_mcmc + '\n')

    unfiltered_name_writer.close()
    unfiltered_dist_writer.close()
    unfiltered_cbmc_writer.close()
    unfiltered_mcmc_writer.close()
    unfiltered_label_writer.close()


def write_testing_data(testing_project):
    testing_data = process_testing(ROOT_PATH + 'data/testing-data/' + testing_project + '_without_filtering.json')
    for data in testing_data:
        project = data['name']
        features = data['features']
        if not os.path.exists(TESTING_DATA_PATH + project):
            os.makedirs(TESTING_DATA_PATH + project)
        name_file = TESTING_DATA_PATH + project + '/test_name.txt'
        dist_file = TESTING_DATA_PATH + project + '/test_dist.txt'
        cbmc_file = TESTING_DATA_PATH + project + '/test_cbmc.txt'
        mcmc_file = TESTING_DATA_PATH + project + '/test_mcmc.txt'

        name_writer = open(name_file, 'w', encoding='utf-8')
        dist_writer = open(dist_file, 'w', encoding='utf-8')
        cbmc_writer = open(cbmc_file, 'w', encoding='utf-8')
        mcmc_writer = open(mcmc_file, 'w', encoding='utf-8')

        num = 0
        for i in range(len(features)):
            feature = features[i]
            len_target = len(feature['target_class_list'])
            if len_target == 0:
                continue
            num += 1
            for j in range(len_target):
                method_name = feature['method_name']
                source_class_name = feature['source_class_name']
                target_class_name = feature['target_class_list'][j]
                name_writer.write(str(num) + ' ')
                for word in method_name:
                    name_writer.write(word + ' ')
                for word in source_class_name:
                    name_writer.write(word + ' ')
                for word in target_class_name:
                    name_writer.write(word + ' ')
                name_writer.write('\n')
                source_dist = feature['source_dist']
                target_dist = feature['target_dist_list'][j]
                dist_writer.write(str(num) + ' ' + source_dist + ' ' + target_dist + '\n')
                source_cbmc = feature['source_cbmc']
                target_cbmc = feature['target_cbmc_list'][j]
                cbmc_writer.write(str(num) + ' ' + source_cbmc + ' ' + target_cbmc + '\n')
                source_mcmc = feature['source_mcmc']
                target_mcmc = feature['target_mcmc_list'][j]
                mcmc_writer.write(str(num) + ' ' + source_mcmc + ' ' + target_mcmc + '\n')
        name_writer.close()
        dist_writer.close()
        cbmc_writer.close()
        mcmc_writer.close()


def get_root_path(project_name='feTruth'):
    root_path = os.path.abspath(os.path.dirname(__file__)).replace("\\", "/").split(project_name)[0]
    return root_path + project_name + "/"


if __name__ == '__main__':
    ROOT_PATH = get_root_path()
    write_without_filtering_data()
    for testing_project in TESTING_PROJECTS:
        write_testing_data(testing_project)
