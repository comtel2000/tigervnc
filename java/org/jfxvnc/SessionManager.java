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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.paint.Color;

public class SessionManager {


    private String name;
    private final Properties props = new Properties();

    private final Path propPath;

    private SessionManager(String name) {
	this.name = name;
	propPath = FileSystems.getDefault().getPath(System.getProperty("user.home"), "." + name + ".properties");
    }

    private static SessionManager sessionManager;

    public static SessionManager createSessionManager(String name) {
	return sessionManager = new SessionManager(name);
    }

    public static SessionManager getSessionManager() {
	return sessionManager;
    }

    public Properties getProperties() {
	return props;
    }

    public void loadSession() {

	if (Files.exists(propPath, LinkOption.NOFOLLOW_LINKS)) {
	    try (InputStream is = Files.newInputStream(propPath, StandardOpenOption.READ)) {
		props.load(is);
	    } catch (IOException ex) {
		ex.printStackTrace();
	    }
	}
    }

    public void saveSession() {
	try (OutputStream outStream = Files.newOutputStream(propPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
	    props.store(outStream, name + " session properties");
	} catch (IOException ex) {
	    ex.printStackTrace();
	}

    }

    public void bind(final BooleanProperty property, final String propertyName) {
	String value = props.getProperty(propertyName);
	if (value != null) {
	    property.set(Boolean.valueOf(value));
	}
	property.addListener(o -> {
	    props.setProperty(propertyName, property.getValue().toString());
	});
    }

    @SuppressWarnings("unchecked")
    public void bind(final ObjectProperty<?> property, final String propertyName, Class<?> type) {
	String value = props.getProperty(propertyName);
	if (value != null) {
	    if (type.getName().equals(Color.class.getName())) {
		((ObjectProperty<Color>) property).set(Color.valueOf(value));
	    } else if (type.getName().equals(String.class.getName())) {
		((ObjectProperty<String>) property).set(value);
	    } else {
		((ObjectProperty<Object>) property).set(value);
	    }
	}
	property.addListener(o -> {
	    props.setProperty(propertyName, property.getValue().toString());
	});
    }

    public void bind(final DoubleProperty property, final String propertyName) {
	String value = props.getProperty(propertyName);
	if (value != null) {
	    property.set(Double.valueOf(value));
	}
	property.addListener(o -> {
	    props.setProperty(propertyName, property.getValue().toString());
	});
    }

    public void bind(final ToggleGroup toggleGroup, final String propertyName) {
	try {
	    String value = props.getProperty(propertyName);
	    if (value != null) {
		int selectedToggleIndex = Integer.parseInt(value);
		toggleGroup.selectToggle(toggleGroup.getToggles().get(selectedToggleIndex));
	    }
	} catch (Exception ignored) {
	}
	toggleGroup.selectedToggleProperty().addListener(o -> {
	    if (toggleGroup.getSelectedToggle() == null) {
		props.remove(propertyName);
	    } else {
		props.setProperty(propertyName, Integer.toString(toggleGroup.getToggles().indexOf(toggleGroup.getSelectedToggle())));
	    }
	});
    }

    public void bind(final Accordion accordion, final String propertyName) {
	Object selectedPane = props.getProperty(propertyName);
	for (TitledPane tp : accordion.getPanes()) {
	    if (tp.getText() != null && tp.getText().equals(selectedPane)) {
		accordion.setExpandedPane(tp);
		break;
	    }
	}
	accordion.expandedPaneProperty().addListener((ov, t, expandedPane) -> {
	    if (expandedPane != null) {
		props.setProperty(propertyName, expandedPane.getText());
	    }
	});
    }

    public void bind(final StringProperty property, final String propertyName) {
	String value = props.getProperty(propertyName);
	if (value != null) {
	    property.set(value);
	}

	property.addListener(o -> {
	    props.setProperty(propertyName, property.getValue());
	});
    }

}
