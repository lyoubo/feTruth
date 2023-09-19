import json

import joblib


def get_feature(row):
    feature = []
    source_invocation_nums = row['source_invocation_nums']
    target_invocation_nums = row['target_invocation_nums']
    matched_invocation_nums = row['matched_invocation_nums']
    source_code_elements = row['source_code_elements']
    target_code_elements = row['target_code_elements']
    matched_code_elements = row['matched_code_elements']
    feature.append(
        0 if source_invocation_nums == 0 else float(matched_invocation_nums) / float(source_invocation_nums))
    feature.append(0 if target_invocation_nums == 0 else float(matched_invocation_nums) / float(target_invocation_nums))
    feature.append(int(matched_code_elements))
    feature.append(0 if source_code_elements == 0 else float(matched_code_elements) / float(source_code_elements))
    feature.append(0 if target_code_elements == 0 else float(matched_code_elements) / float(target_code_elements))
    return feature


def main():
    data = []
    refactoring_ids = []
    dt = joblib.load('dt.pkl')
    f = open('../dataset/raw_data/decision_feature.json', encoding='utf-8')
    records = json.load(f)['RECORDS']
    f.close()
    for row in records:
        data.append(get_feature(row))
        refactoring_ids.append(row['refactoring_id'])
    y_predict = dt.predict(data)
    maps = dict(zip(refactoring_ids, y_predict))

    f = open('../dataset/raw_data/positive_examples.json', encoding='utf-8')
    content = json.load(f)
    f.close()
    records = content['RECORDS']
    record_id = 0
    new_records = []
    for record in records:
        refactoring_id = record['refactoring_id']
        if refactoring_id in maps and maps[refactoring_id] == 1:
            record_id += 1
            record['id'] = record_id
            new_records.append(record)
    content['RECORDS'] = new_records
    with open('../dataset/training_data/positive_samples.json', 'w', encoding='utf-8') as f:
        json.dump(content, f, indent=2)

    f = open('../dataset/raw_data/negative_examples.json', encoding='utf-8')
    content = json.load(f)
    f.close()
    records = content['RECORDS']
    record_id = 0
    new_records = []
    for record in records:
        refactoring_id = record['refactoring_id']
        if refactoring_id in maps and maps[refactoring_id] == 1:
            record_id += 1
            record['id'] = record_id
            new_records.append(record)
    content['RECORDS'] = new_records
    with open('../dataset/training_data/negative_samples.json', 'w', encoding='utf-8') as f:
        json.dump(content, f, indent=2)


if __name__ == '__main__':
    main()
