package org.datageneration.service;

import java.sql.SQLException;
import java.util.List;

public interface TestingDataService {

    void generateData(List<String> projects) throws SQLException;
}
