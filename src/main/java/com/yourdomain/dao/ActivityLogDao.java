package com.yourdomain.dao;

import com.yourdomain.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    // INSERT

    public void log(Integer userId, String action)
            throws SQLException {

        String sql = """
                INSERT INTO activity_logs(user_id, action)
                VALUES (?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (userId == null) {
                statement.setNull(1, Types.INTEGER);
            } else {
                statement.setInt(1, userId);
            }

            statement.setString(2, action);
            statement.executeUpdate();
        }
    }

    // FETCH RECENT (ALL USERS)

    public List<ActivityLog> findRecent(int limit)
            throws SQLException {

        String sql = """
                SELECT al.id, al.user_id, u.username, al.action, al.created_at
                FROM activity_logs al
                LEFT JOIN users u ON al.user_id = u.id
                ORDER BY al.id DESC
                LIMIT ?
                """;

        return query(sql, null, limit);
    }

    // FETCH RECENT (ONE USER)

    public List<ActivityLog> findByUser(int userId, int limit)
            throws SQLException {

        String sql = """
                SELECT al.id, al.user_id, u.username, al.action, al.created_at
                FROM activity_logs al
                LEFT JOIN users u ON al.user_id = u.id
                WHERE al.user_id = ?
                ORDER BY al.id DESC
                LIMIT ?
                """;

        return query(sql, userId, limit);
    }

    // SHARED QUERY

    private List<ActivityLog> query(
            String sql,
            Integer userId,
            int limit
    ) throws SQLException {

        List<ActivityLog> logs = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            int index = 1;

            if (userId != null) {
                statement.setInt(index++, userId);
            }

            statement.setInt(index, limit);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {
                    logs.add(mapRow(result));
                }
            }
        }

        return logs;
    }

    // MAP ROW

    private ActivityLog mapRow(ResultSet result)
            throws SQLException {

        int rawUserId = result.getInt("user_id");
        Integer userId = result.wasNull() ? null : rawUserId;

        return new ActivityLog(
                result.getInt("id"),
                userId,
                result.getString("username"),
                result.getString("action"),
                result.getString("created_at")
        );
    }
}