package com.yourdomain.ui;

import java.util.Scanner;
import java.util.Set;

final class Prompts {

    private static final Set<String> VALID_ROLES =
            Set.of("STUDENT", "TEACHER", "LIBRARIAN", "ADMIN");

    private static final Set<String> VALID_STATUSES =
            Set.of("APPROVED", "PENDING");

    private Prompts() {
    }

    // INTEGER INPUT

    static int readInt(Scanner scanner, String prompt) {

        while (true) {

            System.out.print(prompt);

            String input = scanner.nextLine().trim();

            try {
                return Integer.parseInt(input);

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid input. Please enter numbers only (or 0 to cancel)."
                );
            }
        }
    }

    // VALID ROLE

    static String readRole(Scanner scanner) {

        while (true) {

            System.out.println("\nAvailable roles:");
            System.out.println("STUDENT");
            System.out.println("TEACHER");
            System.out.println("LIBRARIAN");
            System.out.println("ADMIN");
            System.out.println("Enter 0 to cancel.");

            System.out.print("Enter new role: ");

            String role = scanner.nextLine()
                    .trim()
                    .toUpperCase();

            if (role.equals("0")) {
                return null;
            }

            if (VALID_ROLES.contains(role)) {
                return role;
            }

            System.out.println(
                    "Invalid role. Please enter STUDENT, TEACHER, LIBRARIAN, or ADMIN."
            );
        }
    }

    // VALID STATUS

    static String readStatus(Scanner scanner) {

        while (true) {

            System.out.println("\nAvailable statuses:");
            System.out.println("APPROVED");
            System.out.println("PENDING");
            System.out.println("Enter 0 to cancel.");

            System.out.print("Enter new status: ");

            String status = scanner.nextLine()
                    .trim()
                    .toUpperCase();

            if (status.equals("0")) {
                return null;
            }

            if (VALID_STATUSES.contains(status)) {
                return status;
            }

            System.out.println(
                    "Invalid status. Please enter APPROVED or PENDING."
            );
        }
    }
}