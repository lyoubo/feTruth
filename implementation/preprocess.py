import json
import os

MAX_NUM_WORD = 5
TRAINING_DATA_PATH = 'training_data/'
TESTING_DATA_PATH = 'testing_data/'


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


def preprocess_name(sentence):
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


def get_training_features(data):
    features = []
    for row in data:
        if row['refactoring_id']:
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

        method_name = preprocess_name(method_name)
        source_class_name = preprocess_name(source_class_name)
        target_class_name = preprocess_name(target_class_name)
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


def get_testing_features(data):
    features = []
    for row in data:
        source_class_name = row['source_class_name'].split('.')[-1]
        method_name = row['method_name']
        method_signature = row['method_signature']
        source_dist = row['source_dist']
        source_cbmc = row['source_cbmc']
        source_mcmc = row['source_mcmc']
        target_class_list = row['target_class_list'].split(', ')
        for i in range(len(target_class_list)):
            target_class_list[i] = target_class_list[i].split('.')[-1]
        target_dist_list = row['target_dist_list'].split(', ')
        target_cbmc_list = row['target_cbmc_list'].split(', ')
        target_mcmc_list = row['target_mcmc_list'].split(', ')

        source_class_name = preprocess_name(source_class_name)
        for i in range(len(target_class_list)):
            target_class_list[i] = preprocess_name(target_class_list[i])
        method_name = preprocess_name(method_name)
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
    return features


def preprocess_training_data():
    print("Preprocessing training data...")
    with open('../dataset/training_data/positive_samples.json', encoding='utf-8') as f:
        true_positives = json.load(f)['RECORDS']
    with open('../dataset/training_data/negative_samples.json', encoding='utf-8') as f:
        true_negatives = json.load(f)['RECORDS']
    tp_features = get_training_features(true_positives)
    tn_features = get_training_features(true_negatives)
    if not os.path.exists(TRAINING_DATA_PATH + 'real_world'):
        os.makedirs(TRAINING_DATA_PATH + 'real_world')
    real_name_file = TRAINING_DATA_PATH + 'real_world/train_name.txt'
    real_dist_file = TRAINING_DATA_PATH + 'real_world/train_dist.txt'
    real_cbmc_file = TRAINING_DATA_PATH + 'real_world/train_cbmc.txt'
    real_mcmc_file = TRAINING_DATA_PATH + 'real_world/train_mcmc.txt'
    real_label_file = TRAINING_DATA_PATH + 'real_world/train_label.txt'

    real_name_writer = open(real_name_file, 'w', encoding='utf-8')
    real_dist_writer = open(real_dist_file, 'w', encoding='utf-8')
    real_cbmc_writer = open(real_cbmc_file, 'w', encoding='utf-8')
    real_mcmc_writer = open(real_mcmc_file, 'w', encoding='utf-8')
    real_label_writer = open(real_label_file, 'w', encoding='utf-8')

    for i in range(len(tn_features)):
        tp_feature = tp_features[i]
        tn_feature = tn_features[i]

        # write positive samples for feTruth
        label = 1
        real_label_writer.write(str(label) + '\n')
        method_name = tp_feature['method_name']
        source_class_name = tp_feature['source_class_name']
        target_class_name = tp_feature['target_class_name']
        for word in method_name:
            real_name_writer.write(word + ' ')
        for word in source_class_name:
            real_name_writer.write(word + ' ')
        for word in target_class_name:
            real_name_writer.write(word + ' ')
        real_name_writer.write('\n')
        source_dist = tp_feature['source_dist']
        target_dist = tp_feature['target_dist']
        real_dist_writer.write(source_dist + ' ' + target_dist + '\n')
        source_cbmc = tp_feature['source_cbmc']
        target_cbmc = tp_feature['target_cbmc']
        real_cbmc_writer.write(source_cbmc + ' ' + target_cbmc + '\n')
        source_mcmc = tp_feature['source_mcmc']
        target_mcmc = tp_feature['target_mcmc']
        real_mcmc_writer.write(source_mcmc + ' ' + target_mcmc + '\n')

        # write negative samples for feTruth
        label = 0
        real_label_writer.write(str(label) + '\n')
        method_name = tn_feature['method_name']
        source_class_name = tn_feature['source_class_name']
        target_class_name = tn_feature['target_class_name']
        for word in method_name:
            real_name_writer.write(word + ' ')
        for word in source_class_name:
            real_name_writer.write(word + ' ')
        for word in target_class_name:
            real_name_writer.write(word + ' ')
        if i < len(tn_features) - 1:
            real_name_writer.write('\n')
        source_dist = tn_feature['source_dist']
        target_dist = tn_feature['target_dist']
        real_dist_writer.write(source_dist + ' ' + target_dist)
        if i < len(tn_features) - 1:
            real_dist_writer.write('\n')
        source_cbmc = tn_feature['source_cbmc']
        target_cbmc = tn_feature['target_cbmc']
        real_cbmc_writer.write(source_cbmc + ' ' + target_cbmc)
        if i < len(tn_features) - 1:
            real_cbmc_writer.write('\n')
        source_mcmc = tn_feature['source_mcmc']
        target_mcmc = tn_feature['target_mcmc']
        real_mcmc_writer.write(source_mcmc + ' ' + target_mcmc)
        if i < len(tn_features) - 1:
            real_mcmc_writer.write('\n')

    real_name_writer.close()
    real_dist_writer.close()
    real_cbmc_writer.close()
    real_mcmc_writer.close()
    real_label_writer.close()


def preprocess_testing_data(testing_project):
    print("Preprocessing testing data...")
    with open('./testing_data/' + testing_project + '.json', encoding='utf-8') as f:
        testing_data = json.load(f)['RECORDS']
    testing_features = get_testing_features(testing_data)
    if not os.path.exists(TESTING_DATA_PATH + testing_project):
        os.makedirs(TESTING_DATA_PATH + testing_project)
    name_file = TESTING_DATA_PATH + testing_project + '/test_name.txt'
    dist_file = TESTING_DATA_PATH + testing_project + '/test_dist.txt'
    cbmc_file = TESTING_DATA_PATH + testing_project + '/test_cbmc.txt'
    mcmc_file = TESTING_DATA_PATH + testing_project + '/test_mcmc.txt'

    name_writer = open(name_file, 'w', encoding='utf-8')
    dist_writer = open(dist_file, 'w', encoding='utf-8')
    cbmc_writer = open(cbmc_file, 'w', encoding='utf-8')
    mcmc_writer = open(mcmc_file, 'w', encoding='utf-8')

    num = 0
    for i in range(len(testing_features)):
        feature = testing_features[i]
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
            if i < len(testing_features) - 1 or (i == len(testing_features) - 1 and j < len_target - 1):
                name_writer.write('\n')
            source_dist = feature['source_dist']
            target_dist = feature['target_dist_list'][j]
            dist_writer.write(str(num) + ' ' + source_dist + ' ' + target_dist)
            if i < len(testing_features) - 1 or (i == len(testing_features) - 1 and j < len_target - 1):
                dist_writer.write('\n')
            source_cbmc = feature['source_cbmc']
            target_cbmc = feature['target_cbmc_list'][j]
            cbmc_writer.write(str(num) + ' ' + source_cbmc + ' ' + target_cbmc)
            if i < len(testing_features) - 1 or (i == len(testing_features) - 1 and j < len_target - 1):
                cbmc_writer.write('\n')
            source_mcmc = feature['source_mcmc']
            target_mcmc = feature['target_mcmc_list'][j]
            mcmc_writer.write(str(num) + ' ' + source_mcmc + ' ' + target_mcmc)
            if i < len(testing_features) - 1 or (i == len(testing_features) - 1 and j < len_target - 1):
                mcmc_writer.write('\n')

    name_writer.close()
    dist_writer.close()
    cbmc_writer.close()
    mcmc_writer.close()
