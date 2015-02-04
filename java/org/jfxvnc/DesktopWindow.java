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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

import com.tigervnc.rfb.LogWriter;
import com.tigervnc.rfb.ManagedPixelBuffer;
import com.tigervnc.rfb.PixelFormat;
import com.tigervnc.rfb.Rect;

public class DesktopWindow extends Region implements Runnable {

    CConn cc;

    // access to the following must be synchronized:
    PlatformPixelBuffer im;
    Thread setColourMapEntriesTimerThread;

    int cursorPosX, cursorPosY;
    ManagedPixelBuffer cursorBacking;
    int cursorBackingX, cursorBackingY;

    public int scaledWidth = 0, scaledHeight = 0;
    float scaleWidthRatio, scaleHeightRatio;

    // the following are only ever accessed by the GUI thread:
    int lastX, lastY;
    Rect damage = new Rect();

    private BooleanProperty connectedProperty = new SimpleBooleanProperty(false);

    private org.jfxvnc.PointerEventHandler pointerHandler;
    private org.jfxvnc.KeyEventHandler keyHandler;

    static LogWriter vlog = new LogWriter("DesktopWindow.fx");

    public DesktopWindow(int width, int height, PixelFormat serverPF, CConn _cc) {
	super();
	cc = _cc;
	im = new PixelBuffer(width, height, cc, this);
	setWidth(width);
	setHeight(height);
	connectedProperty.set(true);
    }

    public void registerEventHandler() {
	
	setOnMouseEntered((event) -> {
	    requestFocus();
	});

	if (pointerHandler == null) {
	    pointerHandler = new PointerEventHandler();
	    pointerHandler.register(this);
	    pointerHandler.enabledProperty().bind(connectedProperty);
	}
	pointerHandler.setInputEventListener(cc);

	if (keyHandler == null) {
	    keyHandler = new KeyEventHandler();
	    keyHandler.register(this.getScene());
	    keyHandler.enabledProperty().bind(connectedProperty);
	}
	keyHandler.setInputEventListener(cc);
	

    }
    

    public int width() {
	return (int) getWidth();
    }

    public int height() {
	return (int) getHeight();
    }

    public final PixelFormat getPF() {
	return im.getPF();
    }

    public void setServerPF(PixelFormat pf) {
	im.setPF(pf);
    }

    public PixelFormat getPreferredPF() {
	return im.getNativePF();
    }

    // setColourMapEntries() changes some of the entries in the colourmap.
    // Unfortunately these messages are often sent one at a time, so we delay
    // the
    // settings taking effect unless the whole colourmap has changed. This is
    // because getting java to recalculate its internal translation table and
    // redraw the screen is expensive.

    public synchronized void setColourMapEntries(int firstColour, int nColours, int[] rgbs) {
	im.setColourMapEntries(firstColour, nColours, rgbs);
	if (nColours <= 256) {
	    im.updateColourMap();
	} else {
	    if (setColourMapEntriesTimerThread == null) {
		setColourMapEntriesTimerThread = new Thread(this);
		setColourMapEntriesTimerThread.start();
	    }
	}
    }

    // Update the actual window with the changed parts of the framebuffer.
    public void updateWindow() {
	vlog.debug("updateWindow");
	toFront();
    }

    public void setCanvas(Image c) {
	getChildren().clear();
	getChildren().add(new ImageView(c));
    }

    // resize() is called when the desktop has changed size
    public void resize() {
	vlog.debug("resize");
	int w = cc.cp.width;
	int h = cc.cp.height;

	setWidth(w);
	setHeight(h);

	im.resize(w, h);
    }

    public final void fillRect(int x, int y, int w, int h, int pix) {
	vlog.debug("fillRect");
	im.fillRect(x, y, w, h, pix);
    }

    public final void imageRect(int x, int y, int w, int h, Object pix) {
	vlog.debug("imageRect");
	im.imageRect(x, y, w, h, pix);

    }

    public final void copyRect(int x, int y, int w, int h, int srcX, int srcY) {
	vlog.debug("copyRect");
	im.copyRect(x, y, w, h, srcX, srcY);
    }

    public void setScaledSize() {
	String scaleString = cc.viewer.scalingFactor.getValue();
	if (!scaleString.equalsIgnoreCase("Auto") && !scaleString.equalsIgnoreCase("FixedRatio")) {
	    int scalingFactor = Integer.parseInt(scaleString);
	    scaledWidth = (int) Math.floor((float) cc.cp.width * (float) scalingFactor / 100.0);
	    scaledHeight = (int) Math.floor((float) cc.cp.height * (float) scalingFactor / 100.0);
	} else {
	    scaledWidth = cc.cp.width;
	    scaledHeight = cc.cp.height;
	}
	scaleWidthRatio = (float) scaledWidth / (float) cc.cp.width;
	scaleHeightRatio = (float) scaledHeight / (float) cc.cp.height;
    }


    // run() is executed by the setColourMapEntriesTimerThread - it sleeps for
    // 100ms before actually updating the colourmap.
    public synchronized void run() {
	try {
	    Thread.sleep(100);
	} catch (InterruptedException e) {
	}
	im.updateColourMap();
	setColourMapEntriesTimerThread = null;
    }

}
