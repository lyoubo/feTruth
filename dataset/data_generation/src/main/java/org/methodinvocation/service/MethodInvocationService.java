package org.methodinvocation.service;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.sql.SQLException;

public interface MethodInvocationService {

    void findInvocation() throws SQLException, GitAPIException, IOException;

    void matchInvocation() throws SQLException;
}
