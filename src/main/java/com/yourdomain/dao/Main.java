
package com.yourdomain;

import com.yourdomain.dao.DatabaseInitializer;
import com.yourdomain.dao.UserDao;
import com.yourdomain.ui.Authentication;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        // Step 1: Initialize database and required tables.
        DatabaseInitializer initializer = new DatabaseInitializer();
        initializer.initialize();

        // Step 2: Create shared objects.
        UserDao userDao = new UserDao();

        // Step 3: Delegate to Authentication.
        try (Scanner scanner = new Scanner(System.in)) {

            Authentication authentication =
                    new Authentication(userDao, scanner);

            authentication.start();
        }
    }
}
