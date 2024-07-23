package com.iti.serverapplication;



import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

public class ServerController implements Initializable {
    @FXML
    private TextField portField;
    @FXML
    private Button startServerBtn;
    @FXML
    private Button stopServerBtn;
    @FXML
    private Label statusText;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    public void loadTableData(){
        try {

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert a = new Alert(AlertType.ERROR);
            a.setTitle("Failed");
            a.setHeaderText("Connection Failed");
            a.setResizable(true);
            a.setContentText("Connection to Database Failed");
            a.showAndWait();
            System.exit(0);
        }
    }

    public void startServer(ActionEvent ae) {
        if (portField.getText().isEmpty() || !Pattern.matches(
                "^((6553[0-5])|(655[0-2][0-9])|(65[0-4][0-9]{2})|(6[0-4][0-9]{3})|([1-5][0-9]{4})|([0-5]{0,5})|([0-9]{1,4}))$",
                portField.getText())) {
            Alert a = new Alert(AlertType.ERROR);
            a.setTitle("Failed");
            a.setHeaderText("Server Failed to start");
            a.setResizable(true);
            a.setContentText("The port number is not valid");
            a.showAndWait();
            return;
        }
        stopServerBtn.setVisible(true);
        startServerBtn.setVisible(false);
        statusText.setTextFill(Color.GREEN);
        statusText.setText("Online");
        portField.setDisable(true);
        loadTableData();
    }
    private void setServerStopped(){
        stopServerBtn.setVisible(false);
        startServerBtn.setVisible(true);
        statusText.setTextFill(Color.RED);
        statusText.setText("Offline");
        portField.setDisable(false);
    }
    public void stopServer(ActionEvent ae) {
        setServerStopped();
    }
    public void exit(ActionEvent ae) {
        setServerStopped();
        System.exit(0);
    }
}
