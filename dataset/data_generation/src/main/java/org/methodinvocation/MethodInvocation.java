package org.methodinvocation;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.methodinvocation.service.MethodInvocationService;
import org.methodinvocation.service.MethodInvocationServiceImpl;

import java.io.IOException;
import java.sql.SQLException;

public class MethodInvocation {

    public static void main(String[] args) throws SQLException, GitAPIException, IOException {
        MethodInvocationService service = new MethodInvocationServiceImpl();
        service.findInvocation();
        service.matchInvocation();
    }
}
