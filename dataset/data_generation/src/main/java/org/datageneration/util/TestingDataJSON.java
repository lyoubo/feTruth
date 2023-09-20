package org.datageneration.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestingDataJSON {

    private List<Record> RECORDS;

    public TestingDataJSON() {
        this.RECORDS = new ArrayList<>();
    }

    public void populateJSON(String project, List<Map<String, Object>> testingData) {
        int id = 0;
        for (Map<String, Object> data : testingData) {
            id++;
            Record record = new Record(id, project, data);
            RECORDS.add(record);
        }
    }

    class Record {
        private int id;
        private String project_name;
        private String source_class_name;
        private String method_name;
        private String method_signature;
        private String source_dist;
        private String source_cbmc;
        private String source_mcmc;
        private String target_class_list;
        private String target_dist_list;
        private String target_cbmc_list;
        private String target_mcmc_list;

        public Record(int id, String project_name, Map<String, Object> data) {
            this.id = id;
            this.project_name = project_name;
            this.source_class_name = data.get("sourceClassName").toString();
            this.method_name = data.get("methodName").toString();
            this.method_signature = data.get("methodSignature").toString();
            this.source_dist = data.get("sourceDist").toString();
            this.source_cbmc = data.get("sourceCBMC").toString();
            this.source_mcmc = data.get("sourceMCMC").toString();
            this.target_class_list = iterator2String(data.get("targetClassList"));
            this.target_dist_list = iterator2String(data.get("targetDistList"));
            this.target_cbmc_list = iterator2String(data.get("targetCBMCList"));
            this.target_mcmc_list = iterator2String(data.get("targetMCMCList"));
        }

        private String iterator2String(Object obj) {
            List<Object> list = (List<Object>) obj;
            return list.stream().map(Object::toString).collect(Collectors.joining(", "));
        }
    }
}
