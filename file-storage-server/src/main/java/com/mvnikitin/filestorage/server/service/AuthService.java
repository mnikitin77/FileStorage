package com.mvnikitin.filestorage.server.service;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {

    public static boolean authenticate(String username, int password)
            throws SQLException {

        String query = String.format(
                "SELECT COUNT() FROM users WHERE username = '%s'" +
                        " and password = '%d'",
                username, password);
        ResultSet rs = DataService.getData(query);

        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }

    public static boolean register(String username, int password)
            throws SQLException {

        if (!checkIfExists(username)) {
            String statement = String.format(
                    "INSERT INTO users (username, password, directory) " +
                            "VALUES ('%s', '%d', '%s')",
                    username, password, username);

            if (DataService.executeStatement(statement) > 0) {
                return true;
            }
        }

        return false;
    }

    private static boolean checkIfExists(String userName)
            throws SQLException {
        String query = String.format(
                "SELECT COUNT() FROM users WHERE username = '%s'",
                userName);
        ResultSet rs = DataService.getData(query);

        if (rs.next()) {
            return rs.getInt(1) != 0;
        } else
            return false;
    }
}
