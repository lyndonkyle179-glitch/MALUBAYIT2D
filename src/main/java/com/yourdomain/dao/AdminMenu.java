package com.yourdomain.ui;

import com.yourdomain.dao.ActivityLogDao;
import com.yourdomain.dao.DatabaseConnection;
import com.yourdomain.dao.UserDao;
import com.yourdomain.model.ActivityLog;
import com.yourdomain.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private static final int RECENT_ACTIVITY_LIMIT = 50;

    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    private final Scanner scanner;
    private final Runnable viewAllBooks;

    public AdminMenu(
            UserDao userDao,
            ActivityLogDao activityLogDao,
            Scanner scanner,
            Runnable viewAllBooks
    ) {
        this.userDao = userDao;
        this.activityLogDao = activityLogDao;
        this.scanner = scanner;
        this.viewAllBooks = viewAllBooks;
    }

    // ==================== MENU LOOP ====================

    public void start(User currentAdmin) {

        boolean running = true;

        while (running) {

            System.out.println("\n===== ADMIN MENU =====");
            System.out.println("Welcome, " + currentAdmin.username());
            System.out.println("1. View All Users");
            System.out.println("2. Update User Role and Status");
            System.out.println("3. View All Books");
            System.out.println("4. View Activity Logs");
            System.out.println("0. Logout");

            int choice = Prompts.readInt(scanner, "Choose: ");

            switch (choice) {

                case 1 -> viewUsers();

                case 2 -> updateUserRoleAndStatus(currentAdmin);

                case 3 -> viewAllBooks.run();

                case 4 -> viewActivityLogs();

                case 0 -> running = false;

                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // ==================== VIEW USERS ====================

    private void viewUsers() {

        try {

            List<User> users = userDao.getAllUsers();

            System.out.println(
                    "\n================ REGISTERED USERS ================"
            );

            System.out.printf(
                    "%-5s | %-20s | %-12s | %-12s%n",
                    "ID", "USERNAME", "ROLE", "STATUS"
            );

            System.out.println(
                    "---------------------------------------------------"
            );

            if (users.isEmpty()) {

                System.out.println("No registered users found.");
                return;
            }

            for (User user : users) {

                System.out.printf(
                        "%-5d | %-20s | %-12s | %-12s%n",
                        user.id(),
                        user.username(),
                        user.roles(),
                        user.status()
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Unable to load users: " + e.getMessage()
            );
        }
    }

    // ==================== UPDATE USER ROLE AND STATUS ====================

    private void updateUserRoleAndStatus(User currentAdmin) {

        try {

            System.out.println(
                    "\n===== UPDATE USER ROLE AND STATUS ====="
            );

            // Display all registered users.
            viewUsers();

            // Ask for target ID.
            int targetUserId = Prompts.readInt(
                    scanner,
                    "\nEnter target User ID (0 to cancel): "
            );

            // Cancel option.
            if (targetUserId == 0) {

                System.out.println("Update cancelled.");
                return;
            }

            if (targetUserId < 0) {

                System.out.println(
                        "User ID must be greater than zero."
                );

                return;
            }

            // SELF-EDIT SECURITY RESTRICTION.
            if (targetUserId == currentAdmin.id()) {

                System.out.println(
                        "\nWARNING: Modifying the currently active admin account is restricted."
                );

                return;
            }

            // Check whether user exists.
            if (!userExists(targetUserId)) {

                System.out.println(
                        "User ID not found. Update cancelled."
                );

                return;
            }

            // Ask for new role.
            String newRole = Prompts.readRole(scanner);

            if (newRole == null) {

                System.out.println("Update cancelled.");
                return;
            }

            // Ask for new status.
            String newStatus = Prompts.readStatus(scanner);

            if (newStatus == null) {

                System.out.println("Update cancelled.");
                return;
            }

            // Update database.
            boolean updated =
                    userDao.updateUserRoleAndStatus(
                            targetUserId,
                            newRole.toUpperCase(),
                            newStatus.toUpperCase()
                    );

            if (updated) {

                System.out.println(
                        "\nUser role and status updated successfully!"
                );

                System.out.println("User ID: " + targetUserId);
                System.out.println("New Role: " + newRole);
                System.out.println("New Status: " + newStatus);

                activityLogDao.log(
                        currentAdmin.id(), "UPDATE_USER"
                );

            } else {

                System.out.println(
                        "Update failed. User ID was not found."
                );
            }

        } catch (SQLException | IllegalArgumentException e) {

            System.out.println(
                    "Unable to update user: " + e.getMessage()
            );
        }
    }

    // ==================== CHECK USER ID ====================

    private boolean userExists(int userId)
            throws SQLException {

        String sql = "SELECT 1 FROM users WHERE id = ?";

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet result = statement.executeQuery()) {

                return result.next();
            }
        }
    }

    // ==================== AUDIT LOG ====================

    private void viewActivityLogs() {

        try {

            System.out.println("\n===== ACTIVITY LOGS =====");

            System.out.print(
                    "Filter by User ID (press Enter for all): "
            );

            String input = scanner.nextLine().trim();

            List<ActivityLog> logs;

            if (input.isEmpty()) {

                logs = activityLogDao.findRecent(
                        RECENT_ACTIVITY_LIMIT
                );

            } else {

                int userId;

                try {
                    userId = Integer.parseInt(input);

                } catch (NumberFormatException e) {

                    System.out.println("Invalid user ID.");
                    return;
                }

                logs = activityLogDao.findByUser(
                        userId, RECENT_ACTIVITY_LIMIT
                );
            }

            System.out.println(
                    "\nShowing latest " + RECENT_ACTIVITY_LIMIT
                            + " entries (newest first)."
            );

            printLogHeader();

            if (logs.isEmpty()) {

                System.out.println("No activity logs found.");
                return;
            }

            for (ActivityLog log : logs) {
                printLog(log);
            }

        } catch (SQLException e) {

            System.out.println(
                    "Unable to load activity logs: " + e.getMessage()
            );
        }
    }

    private void printLogHeader() {

        System.out.printf(
                "%-5s | %-20s | %-15s | %s%n",
                "ID", "USER", "ACTION", "TIMESTAMP"
        );

        System.out.println(
                "----------------------------------------------------------------------"
        );
    }

    private void printLog(ActivityLog log) {

        String user;

        if (log.username() != null) {

            user = log.username();

        } else if (log.userId() != null) {

            user = "user#" + log.userId();

        } else {

            user = "-";
        }

        System.out.printf(
                "%-5d | %-20s | %-15s | %s%n",
                log.id(),
                user,
                log.action(),
                log.createdAt()
        );
    }
}