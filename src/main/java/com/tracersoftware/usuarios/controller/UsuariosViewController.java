package com.tracersoftware.usuarios.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class UsuariosViewController {

    private final BorderPane host;

    public UsuariosViewController(BorderPane host) {
        this.host = host;
    }

    public void showList() {
        try {
            Node n = FXMLLoader.load(getClass().getResource("/fxml/usuarios_list.fxml"));
            host.setCenter(n);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
