package com.iti.serverapplication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnectionTest {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/testDB";
        String user = "root";
        String password = "root";

        try {
            Connection connection = DriverManager.getConnection(url, user, password);
            System.out.println("Connection successful!");

            // Create a statement
            Statement statement = connection.createStatement();

        /*    // SQL command to create a new database
            String createDatabaseSQL = "CREATE DATABASE IF NOT EXISTS testDB";*/

            // Execute the SQL command
       //     statement.executeUpdate(createDatabaseSQL);
        //    System.out.println("Database created successfully!");

        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }
}
