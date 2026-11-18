package com.yourdomain.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public void initialize() {

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            migrateUsersRoleConstraint(connection);

            String[] tables = {

                    """
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL UNIQUE,
                        password TEXT NOT NULL,
                        roles TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """,

                    """
                    CREATE TABLE IF NOT EXISTS books (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        author TEXT NOT NULL,
                        available_copies INTEGER NOT NULL DEFAULT 1
                    )
                    """,

                    """
                    CREATE TABLE IF NOT EXISTS borrow_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        book_id INTEGER NOT NULL,
                        borrowed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        returned_at TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id),
                        FOREIGN KEY (book_id) REFERENCES books(id)
                    )
                    """,

                    """
                    CREATE TABLE IF NOT EXISTS activity_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER,
                        action TEXT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id)
                    )
                    """
            };

            try (Statement statement = connection.createStatement()) {

                for (String sql : tables) {
                    statement.execute(sql);
                }

                statement.execute("""
                        CREATE INDEX IF NOT EXISTS idx_activity_logs_user
                        ON activity_logs(user_id, id DESC)
                        """);
            }

            System.out.println("Database initialized successfully!");
            System.out.println("SQLite foreign keys are enabled.");

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Database initialization failed: "
                            + e.getMessage(), e
            );
        }
    }

    // MIGRATE OLD USER ROLE CONSTRAINT

    private void migrateUsersRoleConstraint(Connection connection)
            throws SQLException {

        String tableSql = null;

        try (var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT sql
                     FROM sqlite_master
                     WHERE type = 'table' AND name = 'users'
                     """)) {

            if (result.next()) {
                tableSql = result.getString("sql");
            }
        }

        if (tableSql == null ||
                !tableSql.toUpperCase().contains("CHECK")) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF");
        }

        boolean oldAutoCommit = connection.getAutoCommit();

        try {

            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {

                statement.execute(
                        "DROP TABLE IF EXISTS users_new"
                );

                statement.execute("""
                        CREATE TABLE users_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            username TEXT NOT NULL UNIQUE,
                            password TEXT NOT NULL,
                            roles TEXT NOT NULL,
                            status TEXT NOT NULL DEFAULT 'PENDING',
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                        """);

                statement.execute("""
                        INSERT INTO users_new(
                            id, username, password,
                            roles, status, created_at
                        )
                        SELECT
                            id, username, password,
                            roles, status, created_at
                        FROM users
                        """);

                statement.execute("DROP TABLE users");

                statement.execute(
                        "ALTER TABLE users_new RENAME TO users"
                );
            }

            connection.commit();

        } catch (SQLException e) {

            connection.rollback();
            throw e;

        } finally {

            connection.setAutoCommit(oldAutoCommit);

            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
        }
    }
}