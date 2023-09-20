package org.refactoringminer.service;

import java.util.List;

public interface RefactoringMinerService {

    void detectAll(List<String> projects) throws Exception;
}
