package server.database;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.sql.*;

public class UsersDatabase {

    public void createTable() {
        Connection conn = DatabaseConnection.getConnection();

        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "login TEXT NOT NULL UNIQUE," +
                "password TEXT NOT NULL" +
                ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("[DATABASE] Table users created (or not)");
        } catch (SQLException e) {
            System.out.println("[DATABASE] Database error");
            e.printStackTrace();
        }
    }

    public static int register(String username, String password) {
        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        String insertSQL = "INSERT INTO users(login, password) VALUES (?, ?)";
        try (PreparedStatement statement = DatabaseConnection.getConnection().prepareStatement(insertSQL, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, username);
            statement.setString(2, hashedPassword);
            statement.executeUpdate();

            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                } else {
                    throw new SQLException("Cannot fetch ID.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Try again " + e.getMessage());
            return -1;
        }
    }

    public static boolean authenticate(String username, String password) {
        String sql = "SELECT password FROM users WHERE login = ?";

        try (PreparedStatement statement = DatabaseConnection.getConnection().prepareStatement(sql)) {
            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String hashedPasswordFromDB = resultSet.getString("password");
                    BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hashedPasswordFromDB);
                    return result.verified;
                }
            }

        } catch (SQLException e) {
            System.out.println("[DATABASE ERROR] Login error: " + e.getMessage());
        }
        return false;
    }
}