package com.iti.serverapplication;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.iti.serverapplication.ServerController.sockets;
import static com.iti.serverapplication.ServerController.ss;

public class ServerClientHandler implements Runnable {
    private final Socket socket;
    private final Gson gson = new Gson();

    public ServerClientHandler(Socket socket) {
        this.socket = socket;
    }

    static class User {
        private String username;
        private String status;

        public User(String username, String status) {
            this.username = username;
            this.status = status;
        }

        // Getters and Setters
    }

    static class LoginRequest {
        private String action;
        private String email;
        private String password;

        // Getters and Setters
    }

    static class SignupRequest {
        private String action;
        private String username;
        private String email;
        private String password;

        // Getters and Setters
    }

    static class ShowUsersRequest {
        private String action;
        private String email;

        // Getters and Setters
    }

    static class Gamemove {
        private String action;
        @SerializedName("player")
        private String playerName;
        @SerializedName("move")
        private String mov;

        // Getters and Setters
    }

    static class InviteRequest {
        private String action;
        @SerializedName("player")
        private String invitedUsername;
        @SerializedName("player1")
        private String username;
        // Getters and Setters
    }

    static class IncrementScoreRequest {
        private String action;
        @SerializedName("username")
        private String username;
        @SerializedName("score")
        private int score;
    }
    static class InviteResponse {
        @SerializedName("action")
        private String action;
        @SerializedName("player")
        private String invitedUsername;
        @SerializedName("player2")
        private String invitedUsername2;
    }
    static class GenericResponse {
        private boolean success;
        private String message;

        public GenericResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        // Getters and Setters
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
                            boolean isRegistered = isEmailRegistered(loginRequest.email);

                            if (!isRegistered) {
                                responseJson = gson.toJson(new GenericResponse(false, "Email not registered"));
                                output.println(responseJson);
                                output.flush(); // Ensure the response is sent
                            } else {
                                String userStatus = getUserStatusByEmail(loginRequest.email);
                                if ("online".equals(userStatus) || "ingame".equals(userStatus)) {
                                    responseJson = gson.toJson(new GenericResponse(false, "User is already online or in-game"));
                                    output.println(responseJson);
                                    output.flush(); // Ensure the response is sent
                                } else {
                                    boolean success = checkLogin(loginRequest.email, loginRequest.password);
                                    if (success) {
                                        updateStatus(loginRequest.email, "online");
                                        sockets.add(socket);
                                        String socketUserName = getUsername(loginRequest.email);
                                        ss.put(socket, socketUserName);
                                        System.out.println("The map socket: " + ss);
                                        responseJson = gson.toJson(new GenericResponse(true, "Login successful"));
                                    } else {
                                        responseJson = gson.toJson(new GenericResponse(false, "Wrong Password"));
                                    }
                                    output.println(responseJson);
                                    output.flush(); // Ensure the response is sent
                                }
                                System.out.println("The sockets size: " + sockets.size());
                                System.out.println("Login");

                            }
                        }


                        case "signup" -> {
                            System.out.println("Signup");
                            SignupRequest signupRequest = gson.fromJson(requestJson, SignupRequest.class);
                            String[] response = new String[1];
                            boolean success = registerUser(signupRequest.username, signupRequest.email, signupRequest.password,response);
                            responseJson = gson.toJson(new GenericResponse(success, success ? "Signup successful" : response[0]));
                            output.println(responseJson);
                            output.flush(); // Ensure the response is sent
                        }

                        case "showUsers" -> {
                            ShowUsersRequest showUsersRequest = gson.fromJson(requestJson, ShowUsersRequest.class);
                            users = getUsers(showUsersRequest.email);
                            responseJson = gson.toJson(users);
                            output.println(responseJson);
                            output.flush(); // Ensure the response is sent
                        }
                        case "online" -> {
                            String email = (String) requestMap.get("email");
                            boolean success = updateStatus(email, "online");
                            responseJson = gson.toJson(new GenericResponse(success, success ? "Status updated to offline" : "Failed to update status"));
                            output.println(responseJson);
                            output.flush(); // Ensure the response is sent
                        }

                        case "offline" -> {
                            String email = (String) requestMap.get("email");
                            boolean success = updateStatus(email, "offline");
                            responseJson = gson.toJson(new GenericResponse(success, success ? "Status updated to offline" : "Failed to update status"));
                            if (success) {
                                removeSocket(socket);
                            }
                            System.out.println("Sockets size after sign out: " + sockets.size());
                            System.out.println("Signed Out");
                            output.println(responseJson);
                            output.flush(); // Ensure the response is sent
                        }
                        case "invite" -> {
                            InviteRequest inviteRequest = gson.fromJson(requestJson, InviteRequest.class);
                            String[] parts = inviteRequest.username .split(" ", 2); // Split into at most 3 parts
                            int score1 = getScore(parts[1]);
                            int score2 = getScore(inviteRequest.invitedUsername);
                            System.out.println("Score1: " + score1 + " Score2: " + score2);
                            String invitedStatus = getUserStatus(inviteRequest.invitedUsername);
                            //int score2 = getScore(inviteRequest.username);
                            //System.out.println("AWEL SCORE" + score2);
                            System.out.println("a7aaaaaaaaa"+inviteRequest.username);
                            System.out.println("Invited status: " + invitedStatus);
                            if(invitedStatus.equals("offline")) {
                                responseJson = gson.toJson(new GenericResponse(true, invitedStatus));
                            }
                            else if (invitedStatus.equals("ingame"))
                                responseJson = gson.toJson(new GenericResponse(true, invitedStatus));
                            else {    //case online
                                responseJson = gson.toJson(new GenericResponse(true, "online"));
                                System.out.println("inviteee serverrrrrrrr  "+inviteRequest.invitedUsername);
                                System.out.println("username" +inviteRequest.username);
                                //int score1 = getScore(inviteRequest.invitedUsername);
                                //invitedUser(inviteRequest.invitedUsername.toString(),inviteRequest.username,score2,score1);
                                invitedUser(inviteRequest.invitedUsername.toString(),inviteRequest.username,score1,score2);
                            }
                            output.println(responseJson);
                            output.flush(); // Ensure the response is sent
                        }
                        case "getUsername" -> {
                            output.println("getUsername");
                            String email = (String) requestMap.get("email");
                            String username = getUsername(email);
                            responseJson = gson.toJson(new GenericResponse(true, username));
                            output.println(responseJson);
                            output.flush(); // Ensure the response is sent
                        }
                        case "INVITE_ACCEPTED" -> {
                            InviteResponse inviteResponse = gson.fromJson(requestJson, InviteResponse.class);
                            //set2UsersStatusInGame(inviteResponse.invitedUsername,inviteResponse.invitedUsername2);
                            System.out.println(inviteResponse.invitedUsername+"/////////////////"+inviteResponse.invitedUsername2);
                            int score1 = getScore(inviteResponse.invitedUsername);
                            int score2 = getScore(inviteResponse.invitedUsername2);
                            setUserStatusInGame(inviteResponse.invitedUsername);
                            setUserStatusInGame(inviteResponse.invitedUsername2);
                            acceptInvite(inviteResponse.invitedUsername,inviteResponse.invitedUsername2,score1,score2);
                        }

                        case "PlayerMove" ->{
                            Gamemove gamemove = gson.fromJson(requestJson, Gamemove.class);
                            System.out.println(gamemove.playerName + " " + gamemove.mov);
                            gameSession(gamemove.playerName,gamemove.mov);
                        }

                        case "incrementScore" ->{
                            IncrementScoreRequest incrementScoreRequest = gson.fromJson(requestJson, IncrementScoreRequest.class);
                            setScore(incrementScoreRequest.username,incrementScoreRequest.score);
                        }
                        default -> {
                            responseJson = gson.toJson(new GenericResponse(false, "Invalid action"));
                            output.println(responseJson);
                            output.flush(); // Ensure the response is sent
                        }
                    }

                    // Send the response
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                    String errorResponse = gson.toJson(new GenericResponse(false, "Invalid JSON format"));
                    output.println(errorResponse);
                    output.flush(); // Ensure the error response is sent
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            System.err.println( e.getMessage());
        }
        finally {
            // Clean up resources
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Failed to close socket: " + e.getMessage());
            }
        }
    }

    private void setScore(String username,int score)
    {
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "UPDATE users SET score = ? WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, score);
            statement.setString(2, username);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void gameSession(String user,String move) throws IOException {
        for (Socket socket : ss.keySet()) {
            if(Objects.equals(user, ss.get(socket))) {
                PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                pw.println("PlayerMoved " + move);
                pw.flush();
                System.out.println("sentttttttttttttttttt");
            }
        }
    }

    //private void invitedUser(String user,String username,int score2,int score1) throws IOException {
    private void invitedUser(String user,String username,int score1,int score2) throws IOException {
        for (Socket socket : ss.keySet()) {
            System.out.println("the socket is ==== " +ss.get(socket));
            if(Objects.equals(user, ss.get(socket))) {
                System.out.println("invited the userrrrrrrrrr gg ");
                PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                System.out.println("USERNAME INVITTE: ");
                //System.out.println(score1 + "Score2" + score2);
                //pw.println("INVITE " + username + " " + score2 + " " + score1);
                pw.println("INVITE " + username + " " + score1 + " " + score2);
                pw.flush();
            }
        }
    }
    private void setUserStatusInGame(String player1)
    {
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "UPDATE users SET status = ? WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, "ingame");
            statement.setString(2, player1);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    private void acceptInvite(String user,String user2,int score1,int score2) throws IOException {
        for (Socket socket : ss.keySet()) {
            System.out.println("the socket is ==== " +ss.get(socket));
            if(Objects.equals(user, ss.get(socket))) {
                System.out.println("invited the userrrrrrrrrr gg ");
                PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                pw.println("TMAM " + user2 + " " + score1 + " " + score2);
                pw.flush();
            }
        }
    }


    private boolean isEmailRegistered(String email) throws SQLException {
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        }
        return false;
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

    private int getScore(String username)
    {
        Connection connection = DatabaseConnectionManager.getConnection();
        System.out.println(username + "/////////////////////////////////////////////////////////////////////");
        String query = "SELECT score FROM users WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("score");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }
    private boolean registerUser(String username, String email, String password,String[] response) throws SQLException {
        if(isEmailRegistered(email))
        {
            response[0] = "email already exists";
            return false;
        }
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "INSERT INTO users (username, email, password, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            statement.setString(2, email);
            statement.setString(3, password);
            statement.setString(4, "offline");
            return statement.executeUpdate() > 0;
        }
        catch (java.sql.SQLIntegrityConstraintViolationException e)
        {
            response[0] = "username already exists";
            return false;
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
    private String getUserStatusByEmail(String email) {
        Connection connection = DatabaseConnectionManager.getConnection();
        String query = "SELECT status FROM users WHERE email = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
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
    public static void setAllUsersOffline() {
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/tictactoe", "root", "root");
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE users SET status = 'offline'")) {
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getUsername(String email) {
        String username = "Player"; // Default value
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = DatabaseConnectionManager.getConnection();
            String query = "SELECT username FROM users WHERE email = ?"; // Use = for comparison
            statement = connection.prepareStatement(query);
            statement.setString(1, email);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                username = resultSet.getString("username");
            }

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
        return username;
    }

    private void removeSocket(Socket socket) {
        sockets.remove(socket);
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
