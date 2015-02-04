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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Popup;
import javafx.stage.Stage;

import com.tigervnc.rfb.AliasParameter;
import com.tigervnc.rfb.BoolParameter;
import com.tigervnc.rfb.IntParameter;
import com.tigervnc.rfb.LogWriter;
import com.tigervnc.rfb.StringParameter;

public class VncViewerFx extends Application {

    private static final StringProperty titleProperty = new SimpleStringProperty();
    private SessionManager sessionManager;

    static BoolParameter noLionFS = new BoolParameter("NoLionFS", "On Mac systems, setting this parameter will force the use of the old "
	    + "(pre-Lion) full-screen mode, even if the viewer is running on OS X 10.7 Lion or later.", false);
    BoolParameter embed = new BoolParameter("Embed", "If the viewer is being run as an applet, display its output to "
	    + "an embedded frame in the browser window rather than to a dedicated window. Embed=1 implies FullScreen=0 and Scale=100.", false);
    BoolParameter useLocalCursor = new BoolParameter("UseLocalCursor", "Render the mouse cursor locally", true);
    BoolParameter sendLocalUsername = new BoolParameter("SendLocalUsername", "Send the local username for SecurityTypes such as Plain rather than prompting", true);
    StringParameter passwordFile = new StringParameter("PasswordFile", "Password file for VNC authentication", "");
    AliasParameter passwd = new AliasParameter("passwd", "Alias for PasswordFile", passwordFile);
    BoolParameter autoSelect = new BoolParameter("AutoSelect", "Auto select pixel format and encoding", false);
    BoolParameter fullColour = new BoolParameter("FullColour", "Use full colour - otherwise 6-bit colour is used until AutoSelect decides the link is fast enough", true);
    AliasParameter fullColourAlias = new AliasParameter("FullColor", "Alias for FullColour", fullColour);
    IntParameter lowColourLevel = new IntParameter("LowColorLevel",
	    "Color level to use on slow connections. 0 = Very Low (8 colors), 1 = Low (64 colors), 2 = Medium (256 colors)", 2);
    AliasParameter lowColourLevelAlias = new AliasParameter("LowColourLevel", "Alias for LowColorLevel", lowColourLevel);
    StringParameter preferredEncoding = new StringParameter("PreferredEncoding", "Preferred encoding to use (Tight, ZRLE, hextile or raw) - implies AutoSelect=0", "Tight");
    BoolParameter viewOnly = new BoolParameter("ViewOnly", "Don't send any mouse or keyboard events to the server", false);
    BoolParameter shared = new BoolParameter("Shared", "Don't disconnect other viewers upon connection - share the desktop instead", false);
    BoolParameter fullScreen = new BoolParameter("FullScreen", "Full Screen Mode", false);
    BoolParameter acceptClipboard = new BoolParameter("AcceptClipboard", "Accept clipboard changes from the server", true);
    BoolParameter sendClipboard = new BoolParameter("SendClipboard", "Send clipboard changes to the server", true);
    static IntParameter maxCutText = new IntParameter("MaxCutText", "Maximum permitted length of an outgoing clipboard update", 262144);
    StringParameter menuKey = new StringParameter("MenuKey", "The key which brings up the popup menu", "F8");
    StringParameter desktopSize = new StringParameter("DesktopSize", "Reconfigure desktop size on the server on connect (if possible)", "");
    BoolParameter listenMode = new BoolParameter("listen", "Listen for connections from VNC servers", false);
    StringParameter scalingFactor = new StringParameter("ScalingFactor", "Reduce or enlarge the remote desktop image. The value is interpreted as a scaling factor "
	    + "in percent. If the parameter is set to \"Auto\", then automatic scaling is performed. Auto-scaling tries to choose a "
	    + "scaling factor in such a way that the whole remote desktop will fit on the local screen. If the parameter is set to \"FixedRatio\", "
	    + "then automatic scaling is performed, but the original aspect ratio is preserved.", "100");
    BoolParameter alwaysShowServerDialog = new BoolParameter("AlwaysShowServerDialog", "Always show the server dialog even if a server "
	    + "has been specified in an applet parameter or on the command line", false);
    StringParameter vncServerName = new StringParameter("Server", "The VNC server <host>[:<dpyNum>] or <host>::<port>", null);
    IntParameter vncServerPort = new IntParameter("Port", "The VNC server's port number, assuming it is on the host from which the applet was downloaded", 0);
    BoolParameter acceptBell = new BoolParameter("AcceptBell", "Produce a system beep when requested to by the server.", true);
    StringParameter via = new StringParameter("via", "Automatically create an encrypted TCP tunnel to machine gateway, then use that tunnel to connect "
	    + "to a VNC server running on host. By default, this option invokes SSH local port forwarding and assumes that the SSH client binary is located at "
	    + "/usr/bin/ssh. Note that when using the -via option, the host machine name should be specified from the point of view of the gateway machine. "
	    + "For example, \"localhost\" denotes the gateway, not the machine on which vncviewer was launched. See the System Properties section below for "
	    + "information on configuring the -via option.", null);
    StringParameter tunnelMode = new StringParameter("tunnel", "Automatically create an encrypted TCP tunnel to remote gateway, then use that tunnel to connect "
	    + "to the specified VNC server port on the remote host. See the System Properties section below for information on configuring the -tunnel option.", null);
    BoolParameter customCompressLevel = new BoolParameter("CustomCompressLevel", "Use custom compression level. Default if CompressLevel is specified.", false);
    IntParameter compressLevel = new IntParameter("CompressLevel", "Use specified compression level 0 = Low, 6 = High", 1);
    BoolParameter noJpeg = new BoolParameter("NoJPEG", "Disable lossy JPEG compression in Tight encoding.", false);
    IntParameter qualityLevel = new IntParameter("QualityLevel", "JPEG quality level. 0 = Low, 9 = High", 9);
    StringParameter config = new StringParameter("config", "Specifies a configuration file to load.", null);

    private static LogWriter vlog = new LogWriter("VncViewerFX");

    CConn cc;
    Communication service;

    Group parent;
    Stage stage;
    Scene scene;

    @Override
    public void start(Stage _stage) throws Exception {
	stage = _stage;
	sessionManager = SessionManager.createSessionManager("tigervncfx.app");
	sessionManager.loadSession();

	titleProperty.addListener(l -> Platform.runLater(() -> stage.setTitle(String.format("VNC Viewer.fx (%s)", titleProperty.get()))));
	titleProperty.set(System.getProperty("javafx.runtime.version"));
	stage.getIcons().add(new Image(this.getClass().getResourceAsStream("tigervnc.png")));
	stage.setResizable(true);
	parent = new Group();
	stage.setScene(scene = new Scene(parent));
	stage.centerOnScreen();
	stage.show();

	Popup popup = new Popup();
	popup.centerOnScreen();
	//popup.centerOnScreen();
	FXMLLoader loader = new FXMLLoader(getClass().getResource("connectscreen.fxml"));
	popup.getContent().add((Node) loader.load());
	popup.show(stage, scene.getWidth()/2, scene.getHeight()/2);

	ConnectScreenController controller = loader.getController();
	sessionManager.bind(controller.hostProperty(), "host");
	sessionManager.bind(controller.passwordProperty(), "pwd");

	stage.setOnCloseRequest(e -> {
	    sessionManager.saveSession();
	});

	controller.onConnectActionProperty().set(event -> {
	    popup.hide();
	    String[] hostData = controller.hostProperty().get().split(":");
	    vncServerName.setParam(hostData[0]);
	    vncServerPort.setParam(hostData.length > 1 ? Integer.parseInt(hostData[1]) : 5900);
	    passwd.setParam(controller.passwordProperty().get());

	    if (service != null) {
		service.cancel();
	    }
	    service = new Communication();
	    service.start();
	});

    }

    public void setSize(int w, int h) {
	stage.setHeight(h);
	stage.setWidth(w);
    }

    public void setFullScreen(boolean f) {
	stage.setFullScreen(f);
    }

    @Override
    public void stop() throws Exception {
	if (service != null) {
	    try {
		service.cancel();
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
	super.stop();
    }

    public static void main(String[] args) {
	launch(args);
    }

    class Communication extends Service<Void> {

	@Override
	protected Task<Void> createTask() {
	    return new Task<Void>() {
		@Override
		protected Void call() throws java.lang.Exception {
		    try {
			cc = new CConn(VncViewerFx.this);
			vlog.info("connect to host " + vncServerName.getValue() + ":" + vncServerPort.getValue());
			cc.init(vncServerName.getValue(), vncServerPort.getValue());
			while (!cc.shuttingDown) {
			    cc.processMsg();
			}

		    } catch (java.lang.Exception e) {
			e.printStackTrace();
		    }
		    return null;
		}

	    };
	}
    }

    public static StringProperty titleProperty() {
	return titleProperty;
    }

}
