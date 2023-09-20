package org.datageneration.service;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.sql.SQLException;

public interface TrainingDataService {

    void generateTP() throws SQLException, GitAPIException, IOException;

    void generateTN() throws SQLException, GitAPIException, IOException;
}
