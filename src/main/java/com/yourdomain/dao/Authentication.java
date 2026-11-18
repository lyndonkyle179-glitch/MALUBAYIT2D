package com.yourdomain.ui;

import com.yourdomain.dao.ActivityLogDao;
import com.yourdomain.dao.DatabaseConnection;
import com.yourdomain.dao.UserDao;
import com.yourdomain.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Scanner;

public class Authentication {

    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    private final Scanner scanner;

    public Authentication(
            UserDao userDao,
            ActivityLogDao activityLogDao,
            Scanner scanner
    ) {
        this.userDao = userDao;
        this.activityLogDao = activityLogDao;
        this.scanner = scanner;
    }

    // ==================== MAIN MENU ====================

    public void start() {

        boolean running = true;

        while (running) {

            System.out.println("\n===== SCHOOL LIBRARY SYSTEM =====");
            System.out.println("1. Register Account");
            System.out.println("2. Login");
            System.out.println("0. Exit");

            int choice = Prompts.readInt(scanner, "Choose: ");

            switch (choice) {

                case 1 -> register();

                case 2 -> login();

                case 0 -> running = false;

                default -> System.out.println(
                        "Invalid choice. Please choose 1, 2, or 0."
                );
            }
        }

        System.out.println("Application closed.");
    }

    // ==================== REGISTRATION ====================

    private void register() {

        try {

            System.out.println("\n===== REGISTRATION =====");

            System.out.print("Enter username (0 to cancel): ");
            String username = scanner.nextLine().trim();

            if (username.equals("0")) {
                System.out.println("Registration cancelled.");
                return;
            }

            System.out.print("Enter password (0 to cancel): ");
            String password = scanner.nextLine();

            if (password.equals("0")) {
                System.out.println("Registration cancelled.");
                return;
            }

            if (username.isBlank() || password.isBlank()) {

                System.out.println("Fields cannot be empty.");
                return;
            }

            String role = Prompts.readRole(scanner);

            if (role == null) {
                System.out.println("Registration cancelled.");
                return;
            }

            boolean success = userDao.register(
                    username, password, role
            );

            if (success) {

                System.out.println("\nRegistration successful!");
                System.out.println("Username: " + username);
                System.out.println("Role: " + role);
                System.out.println("Status: PENDING");

                System.out.println(
                        "Wait for an administrator to approve your account."
                );

            } else {

                System.out.println("Username already exists.");
            }

        } catch (SQLException | IllegalArgumentException e) {

            System.out.println(
                    "Registration error: " + e.getMessage()
            );
        }
    }

    // ==================== LOGIN ====================

    private void login() {

        try {

            System.out.println("\n===== LOGIN =====");

            System.out.print("Username: ");
            String username = scanner.nextLine().trim();

            System.out.print("Password: ");
            String password = scanner.nextLine();

            Optional<User> result =
                    userDao.login(username, password);

            if (result.isEmpty()) {

                System.out.println(
                        "Invalid username or password."
                );

                return;
            }

            User user = result.get();

            if (!"APPROVED".equalsIgnoreCase(user.status())) {

                System.out.println(
                        "Your account is " + user.status() + "."
                );

                System.out.println(
                        "Please contact the administrator."
                );

                return;
            }

            activityLogDao.log(user.id(), "LOGIN");

            System.out.println("Login successful!");

            switch (user.roles().toUpperCase()) {

                case "ADMIN" -> adminMenu(user);

                case "LIBRARIAN", "TEACHER" -> librarianMenu(user);

                case "STUDENT" -> studentMenu(user);

                default -> System.out.println("Unknown user role.");
            }

        } catch (SQLException e) {

            System.out.println(
                    "Login error: " + e.getMessage()
            );
        }
    }

    // ==================== ADMIN MENU ====================

    private void adminMenu(User currentAdmin) {

        AdminMenu adminMenu = new AdminMenu(
                userDao,
                activityLogDao,
                scanner,
                this::viewAllBooks
        );

        adminMenu.start(currentAdmin);

        logOut(currentAdmin);
    }

    // ==================== LIBRARIAN MENU ====================

    private void librarianMenu(User user) {

        boolean running = true;

        while (running) {

            System.out.println("\n===== LIBRARIAN MENU =====");
            System.out.println("Welcome, " + user.username());
            System.out.println("1. View Available Books");
            System.out.println("2. View All Books");
            System.out.println("3. View Borrowing Records");
            System.out.println("0. Logout");

            int choice = Prompts.readInt(scanner, "Choose: ");

            switch (choice) {

                case 1 -> viewAvailableBooks();

                case 2 -> viewAllBooks();

                case 3 -> viewAllBorrowRecords();

                case 0 -> running = false;

                default -> System.out.println("Invalid choice.");
            }
        }

        logOut(user);
    }

    // ==================== STUDENT MENU ====================

    private void studentMenu(User user) {

        boolean running = true;

        while (running) {

            System.out.println("\n===== STUDENT MENU =====");
            System.out.println("Welcome, " + user.username());
            System.out.println("--------------------------------");
            System.out.println("1. View Available Books");
            System.out.println("2. Search for a Book");
            System.out.println("3. Borrow a Book");
            System.out.println("4. Return a Book");
            System.out.println("5. View My Borrowed Books");
            System.out.println("0. Logout");
            System.out.println("--------------------------------");

            int choice = Prompts.readInt(
                    scanner, "Choose an option: "
            );

            switch (choice) {

                case 1 -> viewAvailableBooks();

                case 2 -> searchBook();

                case 3 -> borrowBook(user);

                case 4 -> returnBook(user);

                case 5 -> viewMyBorrowedBooks(user);

                case 0 -> running = false;

                default -> System.out.println("Invalid choice.");
            }
        }

        logOut(user);
    }

    // ==================== LOGOUT ====================

    private void logOut(User user) {

        try {

            activityLogDao.log(user.id(), "LOGOUT");

        } catch (SQLException e) {

            System.out.println(
                    "Could not save logout activity: " + e.getMessage()
            );
        }

        System.out.println("Logged out successfully.");
    }

    // ==================== BOOK DISPLAY ====================

    private void viewAvailableBooks() {
        displayBooks(true);
    }

    private void viewAllBooks() {
        displayBooks(false);
    }

    private void displayBooks(boolean availableOnly) {

        String sql = availableOnly
                ? """
                  SELECT id, title, author, available_copies
                  FROM books
                  WHERE available_copies > 0
                  ORDER BY title COLLATE NOCASE
                  """
                : """
                  SELECT id, title, author, available_copies
                  FROM books
                  ORDER BY title COLLATE NOCASE
                  """;

        System.out.println(
                availableOnly
                        ? "\n===== AVAILABLE BOOKS ====="
                        : "\n===== ALL BOOKS ====="
        );

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql);

             ResultSet result = statement.executeQuery()) {

            printBookHeader();

            boolean found = false;

            while (result.next()) {

                found = true;
                printBook(result);
            }

            if (!found) {
                System.out.println("No books found.");
            }

        } catch (SQLException e) {

            System.out.println(
                    "Unable to load books: " + e.getMessage()
            );
        }
    }

    private void printBookHeader() {

        System.out.printf(
                "%-5s | %-35s | %-25s | %s%n",
                "ID",
                "TITLE",
                "AUTHOR",
                "AVAILABLE COPIES"
        );

        System.out.println(
                "-------------------------------------------------------------------------------"
        );
    }

    private void printBook(ResultSet result)
            throws SQLException {

        System.out.printf(
                "%-5d | %-35s | %-25s | %d%n",
                result.getInt("id"),
                result.getString("title"),
                result.getString("author"),
                result.getInt("available_copies")
        );
    }

    // ==================== SEARCH BOOK ====================

    private void searchBook() {

        System.out.println("\n===== SEARCH FOR A BOOK =====");

        System.out.print(
                "Enter title or author (0 to cancel): "
        );

        String keyword = scanner.nextLine().trim();

        if (keyword.equals("0")) {

            System.out.println("Search cancelled.");
            return;
        }

        if (keyword.isBlank()) {

            System.out.println(
                    "Search field cannot be empty."
            );

            return;
        }

        String sql = """
                SELECT id, title, author, available_copies
                FROM books
                WHERE title LIKE ? OR author LIKE ?
                ORDER BY title COLLATE NOCASE
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";

            statement.setString(1, pattern);
            statement.setString(2, pattern);

            try (ResultSet result = statement.executeQuery()) {

                System.out.println("\n===== SEARCH RESULTS =====");

                printBookHeader();

                boolean found = false;

                while (result.next()) {

                    found = true;
                    printBook(result);
                }

                if (!found) {

                    System.out.println(
                            "No matching books found."
                    );
                }
            }

        } catch (SQLException e) {

            System.out.println(
                    "Search error: " + e.getMessage()
            );
        }
    }

    // ==================== BORROW BOOK ====================

    private void borrowBook(User user) {

        System.out.println("\n===== BORROW A BOOK =====");

        viewAvailableBooks();

        int bookId = Prompts.readInt(
                scanner,
                "\nEnter Book ID to borrow (0 to cancel): "
        );

        if (bookId == 0) {

            System.out.println("Borrowing cancelled.");
            return;
        }

        if (bookId < 0) {

            System.out.println("Invalid book ID.");
            return;
        }

        String updateSql = """
                UPDATE books
                SET available_copies = available_copies - 1
                WHERE id = ? AND available_copies > 0
                """;

        String insertSql = """
                INSERT INTO borrow_records(user_id, book_id)
                VALUES (?, ?)
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {

                int updated;

                try (PreparedStatement statement =
                             connection.prepareStatement(updateSql)) {

                    statement.setInt(1, bookId);

                    updated = statement.executeUpdate();
                }

                if (updated == 0) {

                    connection.rollback();

                    System.out.println(
                            "Book not found or no copies are available."
                    );

                    return;
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(insertSql)) {

                    statement.setInt(1, user.id());
                    statement.setInt(2, bookId);

                    statement.executeUpdate();
                }

                connection.commit();

                System.out.println(
                        "Book borrowed successfully!"
                );

                activityLogDao.log(
                        user.id(), "BORROW_BOOK"
                );

            } catch (SQLException e) {

                connection.rollback();

                System.out.println(
                        "Borrowing failed: " + e.getMessage()
                );

            } finally {

                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {

            System.out.println(
                    "Database error: " + e.getMessage()
            );
        }
    }

    // ==================== RETURN BOOK ====================

    private void returnBook(User user) {

        System.out.println("\n===== RETURN A BOOK =====");

        viewMyBorrowedBooks(user);

        int recordId = Prompts.readInt(
                scanner,
                "\nEnter Borrow Record ID to return (0 to cancel): "
        );

        if (recordId == 0) {

            System.out.println("Return cancelled.");
            return;
        }

        if (recordId < 0) {

            System.out.println("Invalid record ID.");
            return;
        }

        String selectSql = """
                SELECT book_id
                FROM borrow_records
                WHERE id = ? AND user_id = ? AND returned_at IS NULL
                """;

        String returnSql = """
                UPDATE borrow_records
                SET returned_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ? AND returned_at IS NULL
                """;

        String quantitySql = """
                UPDATE books
                SET available_copies = available_copies + 1
                WHERE id = ?
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {

                int bookId;

                try (PreparedStatement statement =
                             connection.prepareStatement(selectSql)) {

                    statement.setInt(1, recordId);
                    statement.setInt(2, user.id());

                    try (ResultSet result = statement.executeQuery()) {

                        if (!result.next()) {

                            connection.rollback();

                            System.out.println(
                                    "Active borrowing record not found."
                            );

                            return;
                        }

                        bookId = result.getInt("book_id");
                    }
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(returnSql)) {

                    statement.setInt(1, recordId);
                    statement.setInt(2, user.id());

                    if (statement.executeUpdate() != 1) {

                        connection.rollback();

                        System.out.println(
                                "Return was not recorded."
                        );

                        return;
                    }
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(quantitySql)) {

                    statement.setInt(1, bookId);
                    statement.executeUpdate();
                }

                connection.commit();

                System.out.println(
                        "Book returned successfully!"
                );

                activityLogDao.log(
                        user.id(), "RETURN_BOOK"
                );

            } catch (SQLException e) {

                connection.rollback();

                System.out.println(
                        "Return failed: " + e.getMessage()
                );

            } finally {

                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {

            System.out.println(
                    "Database error: " + e.getMessage()
            );
        }
    }

    // ==================== STUDENT BORROWING HISTORY ====================

    private void viewMyBorrowedBooks(User user) {

        String sql = """
                SELECT br.id AS record_id,
                       b.title,
                       b.author,
                       br.borrowed_at,
                       br.returned_at
                FROM borrow_records br
                JOIN books b ON br.book_id = b.id
                WHERE br.user_id = ?
                ORDER BY br.id DESC
                """;

        System.out.println(
                "\n===== MY BORROWED BOOKS ====="
        );

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, user.id());

            try (ResultSet result = statement.executeQuery()) {

                System.out.println(
                        "RECORD ID | TITLE | BORROWED AT | STATUS"
                );

                boolean found = false;

                while (result.next()) {

                    found = true;

                    String returnedAt =
                            result.getString("returned_at");

                    String status = returnedAt == null
                            ? "BORROWED"
                            : "RETURNED";

                    System.out.printf(
                            "%d | %s | %s | %s%n",
                            result.getInt("record_id"),
                            result.getString("title"),
                            result.getString("borrowed_at"),
                            status
                    );

                    if (returnedAt != null) {

                        System.out.println(
                                "  Returned at: " + returnedAt
                        );
                    }
                }

                if (!found) {

                    System.out.println(
                            "You have no borrowing records."
                    );
                }
            }

        } catch (SQLException e) {

            System.out.println(
                    "Unable to load borrowing history: "
                            + e.getMessage()
            );
        }
    }

    // ==================== LIBRARIAN BORROWING RECORDS ====================

    private void viewAllBorrowRecords() {

        String sql = """
                SELECT br.id,
                       u.username,
                       b.title,
                       br.borrowed_at,
                       br.returned_at
                FROM borrow_records br
                JOIN users u ON br.user_id = u.id
                JOIN books b ON br.book_id = b.id
                ORDER BY br.id DESC
                """;

        System.out.println(
                "\n===== ALL BORROWING RECORDS ====="
        );

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql);

             ResultSet result = statement.executeQuery()) {

            boolean found = false;

            while (result.next()) {

                found = true;

                String returnedAt =
                        result.getString("returned_at");

                String status = returnedAt == null
                        ? "BORROWED"
                        : "RETURNED";

                System.out.printf(
                        "Record: %d | User: %s | Book: %s | Borrowed: %s | Status: %s%n",
                        result.getInt("id"),
                        result.getString("username"),
                        result.getString("title"),
                        result.getString("borrowed_at"),
                        status
                );

                if (returnedAt != null) {

                    System.out.println(
                            "  Returned at: " + returnedAt
                    );
                }
            }

            if (!found) {

                System.out.println(
                        "No borrowing records found."
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "Unable to load records: " + e.getMessage()
            );
        }
    }
}