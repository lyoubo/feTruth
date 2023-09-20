package org.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class JDBCUtils {

    private static final String driverClass;
    private static final String url;
    private static final String username;
    private static final String password;

    static {
        ClassLoader classLoader = JDBCUtils.class.getClassLoader();
        URL resource = classLoader.getResource("config.properties");
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(resource.getFile()));
        } catch (IOException ignored) {
            throw new RuntimeException("can't locate config.properties");
        }
        driverClass = props.getProperty("driverClass");
        url = props.getProperty("url");
        username = props.getProperty("username");
        password = props.getProperty("password");
    }

    public static Connection getConnection() {
        try {
            Class.forName(driverClass);
            return DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void close(Connection con, Statement... stmt) throws SQLException {
        if (con != null)
            con.close();
        for (Statement statement : stmt)
            if (statement != null)
                statement.close();
    }
}
