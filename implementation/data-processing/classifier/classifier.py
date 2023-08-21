import csv
import json
import os

import joblib

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'


def get_train():
    ground_truth = []
    dict_label = {}
    with open('empirical_study_label.csv', encoding='utf-8') as f:
        reader = csv.reader(f)
        next(reader)
        rows = [row for row in reader]
        for row in rows:
            refactor_id = int(row[0])
            label = int(row[1])
            if label != 2:
                ground_truth.append(refactor_id)
                dict_label[refactor_id] = label
    train_data = []
    train_label = []
    with open('decision_feature.json', encoding='utf-8') as f:
        results = json.load(f)
        for row in results:
            if row['refactor_id'] in ground_truth:
                train_data.append(get_feature(row))
                train_label.append(dict_label[row['refactor_id']])
    return train_data, train_label


def get_test():
    ground_truth = []
    dict_label = {}
    with open('sample_label.csv', encoding='utf-8') as f:
        reader = csv.reader(f)
        next(reader)
        rows = [row for row in reader]
        for row in rows:
            refactor_id = int(row[0])
            label = int(row[1])
            if label != 2:
                ground_truth.append(refactor_id)
                dict_label[refactor_id] = label
    test_data = []
    test_label = []
    with open('decision_feature.json', encoding='utf-8') as f:
        results = json.load(f)
        for row in results:
            if row['refactor_id'] in ground_truth:
                test_data.append(get_feature(row))
                test_label.append(dict_label[row['refactor_id']])
    return test_data, test_label


def get_feature(row):
    feature = []
    original_invocation_nums = row['original_invocation_nums']
    moved_invocation_nums = row['moved_invocation_nums']
    matched_invocation_nums = row['matched_invocation_nums']
    original_code_elements = row['original_code_elements']
    moved_code_elements = row['moved_code_elements']
    matched_code_elements = row['matched_code_elements']
    feature.append(
        0 if original_invocation_nums == 0 else float(matched_invocation_nums) / float(original_invocation_nums))
    feature.append(0 if moved_invocation_nums == 0 else float(matched_invocation_nums) / float(moved_invocation_nums))
    feature.append(int(matched_code_elements))
    feature.append(0 if original_code_elements == 0 else float(matched_code_elements) / float(original_code_elements))
    feature.append(0 if moved_code_elements == 0 else float(matched_code_elements) / float(moved_code_elements))
    return feature


def classify():
    data = []
    refactor_ids = []
    with open('decision_feature.json', encoding='utf-8') as f:
        results = json.load(f)
        for row in results:
            data.append(get_feature(row))
            refactor_ids.append(row['refactor_id'])
    dt = joblib.load('dt.pkl')
    # train_data, train_label = get_train()
    # dt = DecisionTreeClassifier(criterion='gini', max_features=len(train_data[0]))
    # dt.fit(train_data, train_label)
    y_predict = list(dt.predict(data))
    with open('decision_predict.txt', mode='w', encoding='utf-8') as f:
        for i in range(len(y_predict)):
            f.write(str(refactor_ids[i]) + ',' + str(y_predict[i] + '\n'))


def evaluate():
    test_data, y_hat = get_test()
    dt = joblib.load('dt.pkl')
    y_predict = dt.predict(test_data)
    tp, fp, tn, fn = 0, 0, 0, 0
    for i in range(len(y_predict)):
        if y_hat[i] == y_predict[i] and y_predict[i] == 1:
            tp += 1
        elif y_hat[i] == y_predict[i] and y_predict[i] == 0:
            tn += 1
        elif y_hat[i] != y_predict[i] and y_predict[i] == 1:
            fp += 1
        elif y_hat[i] != y_predict[i] and y_predict[i] == 0:
            fn += 1
    accuracy = 0 if tp + tn + fp + fn == 0 else (tp + tn) * 1.0 / (tp + tn + fp + fn)
    precision = 0 if tp + fp == 0 else tp * 1.0 / (tp + fp)
    recall = 0 if tp + fn == 0 else tp * 1.0 / (tp + fn)
    a = round(accuracy * 100, 2)
    p = round(precision * 100, 2)
    r = round(recall * 100, 2)
    f1 = round(2 * precision * recall / (precision + recall) * 100, 2)
    print('tp: \t\t %d' % tp)
    print('tn: \t\t %d' % tn)
    print('fp: \t\t %d' % fp)
    print('fn: \t\t %d' % fn)
    print('accuracy: \t %.2f%%' % a)
    print('precision: \t %.2f%%' % p)
    print('recall: \t %.2f%%' % r)
    print('f1-score: \t %.2f%%' % f1)


if __name__ == '__main__':
    # get_train()
    # evaluate()
    classify()
