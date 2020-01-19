package com.mvnikitin.filestorage.server.service;

import java.sql.*;

public class DataService {

    private static Connection connection;
    private static Statement stmt;

    public static void connect(String dbDriverClassName, String connectionString) throws SQLException {
        try {
            Class.forName(dbDriverClassName);
            connection = DriverManager.getConnection(connectionString);
            stmt = connection.createStatement();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public synchronized static ResultSet getData(String query) throws SQLException {
        return stmt.executeQuery(query);
    }

    public synchronized static int executeStatement(String statement) throws SQLException {
        stmt.execute(statement);
        return stmt.getUpdateCount();
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
