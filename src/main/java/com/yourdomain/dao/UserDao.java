package com.yourdomain.dao;

import com.yourdomain.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class UserDao {

    private static final Set<String> VALID_ROLES =
            Set.of("STUDENT", "TEACHER", "LIBRARIAN", "ADMIN");

    private static final Set<String> VALID_STATUSES =
            Set.of("APPROVED", "PENDING");

    public boolean register(String username, String password, String role)
            throws SQLException {

        String normalizedRole = normalizeRole(role);

        String sql = """
                INSERT INTO users(username, password, roles, status)
                VALUES (?, ?, ?, 'PENDING')
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username.trim());
            statement.setString(2, password);
            statement.setString(3, normalizedRole);

            statement.executeUpdate();
            return true;

        } catch (SQLException e) {
            if (e.getMessage() != null &&
                    e.getMessage().toLowerCase().contains("unique")) {
                return false;
            }
            throw e;
        }
    }

    public Optional<User> login(String username, String password)
            throws SQLException {

        String sql = """
                SELECT id, username, password, roles, status
                FROM users
                WHERE username = ? AND password = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username.trim());
            statement.setString(2, password);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(new User(
                            result.getInt("id"),
                            result.getString("username"),
                            result.getString("password"),
                            result.getString("roles"),
                            result.getString("status")
                    ));
                }
            }
        }

        return Optional.empty();
    }

    public List<User> getAllUsers() throws SQLException {

        List<User> users = new ArrayList<>();

        String sql = """
                SELECT id, username, roles, status
                FROM users
                ORDER BY id
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                users.add(new User(
                        result.getInt("id"),
                        result.getString("username"),
                        "",
                        result.getString("roles"),
                        result.getString("status")
                ));
            }
        }

        return users;
    }

    // UPDATE USER ROLE AND STATUS

    public boolean updateUserRoleAndStatus(
            int userId,
            String newRole,
            String newStatus
    ) throws SQLException {

        if (userId <= 0) {
            throw new IllegalArgumentException(
                    "User ID must be greater than zero."
            );
        }

        String role = normalizeRole(newRole);
        String status = normalizeStatus(newStatus);

        String sql = """
                UPDATE users
                SET roles = ?, status = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, role);
            statement.setString(2, status);
            statement.setInt(3, userId);

            return statement.executeUpdate() > 0;
        }
    }

    // KEEP OLD STATUS UPDATE METHOD

    public boolean updateStatus(int userId, String status)
            throws SQLException {

        String normalizedStatus = normalizeStatus(status);

        String sql = "UPDATE users SET status = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, normalizedStatus);
            statement.setInt(2, userId);

            return statement.executeUpdate() > 0;
        }
    }

    // VALIDATE ROLE

    private String normalizeRole(String role) {

        String normalized = role == null
                ? ""
                : role.trim().toUpperCase();

        if (!VALID_ROLES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Invalid role: " + role
            );
        }

        return normalized;
    }

    // VALIDATE STATUS

    private String normalizeStatus(String status) {

        String normalized = status == null
                ? ""
                : status.trim().toUpperCase();

        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Invalid status: " + status
            );
        }

        return normalized;
    }
}