package org.datageneration;

import org.datageneration.service.TrainingDataService;
import org.datageneration.service.TrainingDataServiceImpl;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.sql.SQLException;

public class TrainingData {

    public static void main(String[] args) throws SQLException, GitAPIException, IOException {
        TrainingDataService service = new TrainingDataServiceImpl();
        service.generateTP();
        service.generateTN();
    }
}
