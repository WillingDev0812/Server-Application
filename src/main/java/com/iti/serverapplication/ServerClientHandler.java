package com.iti.serverapplication;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServerClientHandler implements Runnable {

    private final Socket socket;
    private final Gson gson = new Gson();

    public ServerClientHandler(Socket socket) {
        this.socket = socket;
    }

    record User(String username, String status) {
    }

    record LoginRequest(String action, String email, String password) {
    }

    record SignupRequest(String action, String username, String email, String password) {
    }

    record ShowUsersRequest(String action, String email) {
    }

    record InviteRequest(String action, String invitedUsername) {
    }

    record GenericResponse(boolean success, String message) {
    }

    List<User> users = new ArrayList<>();

    @Override
    public void run() {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) {

            String requestJson;
            while ((requestJson = input.readLine()) != null) {
                System.out.println("Received JSON: " + requestJson);
                try {
                    Map<String, Object> requestMap = gson.fromJson(requestJson, Map.class);
                    String action = (String) requestMap.get("action");

                    String responseJson;
                    switch (action) {
                        case "login" -> {
                            LoginRequest loginRequest = gson.fromJson(requestJson, LoginRequest.class);
                            boolean success = checkLogin(loginRequest.email(), loginRequest.password());
                            if (success) {
                                updateStatus(loginRequest.email(), "online");
                            }
                            responseJson = gson.toJson(new GenericResponse(success, success ? "Login successful" : "Login failed"));
                        }
                        case "signup" -> {
                            SignupRequest signupRequest = gson.fromJson(requestJson, SignupRequest.class);
                            boolean success = registerUser(signupRequest.username(), signupRequest.email(), signupRequest.password());
                            responseJson = gson.toJson(new GenericResponse(success, success ? "Signup successful" : "Signup failed"));
                        }
                        case "showUsers" -> {
                            ShowUsersRequest showUsersRequest = gson.fromJson(requestJson, ShowUsersRequest.class);
                            users = getUsers(showUsersRequest.email());
                            responseJson = gson.toJson(users);
                        }
                        case "offline" -> {
                            String email = (String) requestMap.get("email");
                            updateStatus(email, "offline");
                            responseJson = gson.toJson(new GenericResponse(true, "Status updated to offline"));
                        }
                        case "invite" -> {
                            InviteRequest inviteRequest = gson.fromJson(requestJson, InviteRequest.class);
                            String invitedStatus = getUserStatus(inviteRequest.invitedUsername());
                            responseJson = gson.toJson(new GenericResponse(true, invitedStatus));
                        }
                        default -> {
                            responseJson = gson.toJson(new GenericResponse(false, "Invalid action"));
                        }
                    }

                    // Send the response
                    output.write(responseJson);
                    output.flush(); // Ensure the response is sent

                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                    String errorResponse = gson.toJson(new GenericResponse(false, "Invalid JSON format"));
                    output.println(errorResponse);
                    output.flush(); // Ensure the error response is sent
                }
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkLogin(String email, String password) throws SQLException {
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "SELECT * FROM users WHERE email = ? AND password = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            statement.setString(2, password);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean updateStatus(String email, String status) throws SQLException {
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "UPDATE users SET status = ? WHERE email = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, status);
            statement.setString(2, email);
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        }
    }

    private boolean registerUser(String username, String email, String password) throws SQLException {
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "INSERT INTO users (username, email, password, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            statement.setString(2, email);
            statement.setString(3, password);
            statement.setString(4, "offline");
            return statement.executeUpdate() > 0;
        }
    }

    private List<User> getUsers(String email) throws SQLException {
        List<User> users = new ArrayList<>();
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "SELECT * FROM users WHERE email != ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String username = resultSet.getString("username");
                    String status = resultSet.getString("status");
                    users.add(new User(username, status));
                }
            }
        }
        return users;
    }

    private String getUsername(String email) {
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "SELECT username FROM users WHERE email = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("username");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Player";
    }

    private String getUserStatus(String invitedUsername) {
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "SELECT status FROM users WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, invitedUsername);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("status");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "offline";
    }
}
