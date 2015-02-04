/* 
 * Copyright (C) 2015 comtel2000
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jfxvnc;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ConnectScreenController implements Initializable {

    @FXML
    private TextField host;
    @FXML
    private PasswordField pwd;
    @FXML
    private Button connect;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public ObjectProperty<EventHandler<ActionEvent>> onConnectActionProperty() {
	return connect.onActionProperty();
    }

    public StringProperty hostProperty() {
	return host.textProperty();
    }

    public StringProperty passwordProperty() {
	return pwd.textProperty();
    }
}
