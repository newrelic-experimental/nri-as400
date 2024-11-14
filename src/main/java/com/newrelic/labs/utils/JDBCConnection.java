package com.newrelic.labs.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JDBCConnection {
    public Connection getJDBCConnection(String systemName, String user, String password, String ssl) throws SQLException {
        String url = "jdbc:as400://" + systemName;
        if ("true".equalsIgnoreCase(ssl)) {
            url += ";secure=true";
        }
        return DriverManager.getConnection(url, user, password);
    }
}