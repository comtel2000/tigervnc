/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * Copyright 2009-2013 Pierre Ossman <ossman@cendio.se> for Cendio AB
 * Copyright (C) 2011-2013 D. R. Commander.  All Rights Reserved.
 * Copyright (C) 2011-2014 Brian P. Hinz
 * Copyright (C) 2015 comtel2000
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */

//
// CConn
//
// Methods on CConn are called from both the GUI thread and the thread which
// processes incoming RFB messages ("the RFB thread").  This means we need to
// be careful with synchronization here.
//
// Any access to writer() must not only be synchronized, but we must also make
// sure that the connection is in RFBSTATE_NORMAL.  We are guaranteed this for
// any code called after serverInit() has been called.  Since the DesktopWindow
// isn't created until then, any methods called only from DesktopWindow can
// assume that we are in RFBSTATE_NORMAL.

package org.jfxvnc;

import java.util.Iterator;

import javafx.application.Platform;

import org.jfxvnc.input.InputEventListener;
import org.jfxvnc.input.InputEventMessage;
import org.jfxvnc.input.KeyEventMessage;
import org.jfxvnc.input.PointerEventMessage;

import com.tigervnc.network.Socket;
import com.tigervnc.network.TcpSocket;
import com.tigervnc.rdr.FdInStreamBlockCallback;
import com.tigervnc.rdr.MemInStream;
import com.tigervnc.rdr.MemOutStream;
import com.tigervnc.rfb.CConnection;
import com.tigervnc.rfb.Encodings;
import com.tigervnc.rfb.Exception;
import com.tigervnc.rfb.LogWriter;
import com.tigervnc.rfb.PixelFormat;
import com.tigervnc.rfb.Point;
import com.tigervnc.rfb.Rect;
import com.tigervnc.rfb.Screen;
import com.tigervnc.rfb.ScreenSet;
import com.tigervnc.rfb.fenceTypes;
import com.tigervnc.rfb.screenTypes;

public class CConn extends CConnection implements FdInStreamBlockCallback, InputEventListener {

    // the following never change so need no synchronization:

    // viewer object is only ever accessed by the GUI thread so needs no
    // synchronization (except for one test in DesktopWindow - see comment
    // there).
    VncViewerFx viewer;

    // access to desktop by different threads is specified in DesktopWindow

    // the following need no synchronization:

    // shuttingDown is set by the GUI thread and only ever tested by the RFB
    // thread after the window has been destroyed.
    boolean shuttingDown = false;

    // reading and writing int and boolean is atomic in java, so no
    // synchronization of the following flags is needed:

    int lowColourLevel;

    // the following are only ever accessed by the GUI thread:
    int buttonMask;

    private String serverHost;
    private int serverPort;
    private Socket sock;

    protected DesktopWindow desktop;

    // FIXME: should be private
    public PixelFormat serverPF;
    private PixelFormat fullColourPF;

    private boolean pendingPFChange;
    private PixelFormat pendingPF;

    private int currentEncoding, lastServerEncoding;

    private boolean formatChange;
    private boolean encodingChange;

    private boolean firstUpdate;
    private boolean pendingUpdate;
    private boolean continuousUpdates;

    private boolean forceNonincremental;

    private boolean supportsSyncFence;

    public int menuKeyCode;
    private boolean fullColour;
    private boolean autoSelect;
    boolean fullScreen;

    static LogWriter vlog = new LogWriter("CConn.fx");

    public final PixelFormat getPreferredPF() {
	return fullColourPF;
    }

    static final PixelFormat verylowColourPF = new PixelFormat(8, 3, false, true, 1, 1, 1, 2, 1, 0);
    static final PixelFormat lowColourPF = new PixelFormat(8, 6, false, true, 3, 3, 3, 4, 2, 0);
    static final PixelFormat mediumColourPF = new PixelFormat(8, 8, false, false, 7, 7, 3, 0, 3, 6);
    static final int KEY_LOC_SHIFT_R = 0;
    static final int KEY_LOC_SHIFT_L = 16;
    static final int SUPER_MASK = 1 << 15;

    public CConn(VncViewerFx _viewer) {
	viewer = _viewer;
    }

    public void init(String vncServerName, int vncServerPort) throws Exception {
	serverHost = vncServerName;
	serverPort = vncServerPort;

	pendingPFChange = false;
	currentEncoding = Encodings.encodingHextile;
	lastServerEncoding = -1;
	fullColour = viewer.fullColour.getValue();
	lowColourLevel = viewer.lowColourLevel.getValue();
	autoSelect = viewer.autoSelect.getValue();
	formatChange = false;
	encodingChange = false;
	fullScreen = viewer.fullScreen.getValue();

	firstUpdate = true;
	pendingUpdate = false;
	continuousUpdates = false;
	forceNonincremental = true;
	supportsSyncFence = false;

	setShared(viewer.shared.getValue());

	String encStr = viewer.preferredEncoding.getValue();
	int encNum = Encodings.encodingNum(encStr);
	if (encNum != -1) {
	    currentEncoding = encNum;
	}
	cp.supportsDesktopResize = true;
	cp.supportsExtendedDesktopSize = true;
	cp.supportsSetDesktopSize = false;
	cp.supportsClientRedirect = true;
	cp.supportsDesktopRename = true;
	cp.supportsLocalCursor = viewer.useLocalCursor.getValue();
	cp.customCompressLevel = viewer.customCompressLevel.getValue();
	cp.compressLevel = viewer.compressLevel.getValue();
	cp.noJpeg = viewer.noJpeg.getValue();
	cp.qualityLevel = viewer.qualityLevel.getValue();

	sock = new TcpSocket(serverHost, serverPort);

	vlog.info("connected to host " + serverHost + " port " + serverPort);

	sock.inStream().setBlockCallback(this);
	setServerName(serverHost);
	setStreams(sock.inStream(), sock.outStream());
	initialiseProtocol();
    }

    public void refreshFramebuffer() {
	forceNonincremental = true;

	// Without fences, we cannot safely trigger an update request directly
	// but must wait for the next update to arrive.
	if (supportsSyncFence)
	    requestNewUpdate();
    }

    public boolean showMsgBox(int flags, String title, String text) {
	// StringBuffer titleText = new StringBuffer("VNC Viewer: "+title);
	return true;
    }

    // deleteWindow() is called when the user closes the desktop or menu
    // windows.

    void deleteWindow() {
    }

    // blockCallback() is called when reading from the socket would block.
    public void blockCallback() {
	try {
	    synchronized (this) {
		wait(1);
	    }
	} catch (java.lang.InterruptedException e) {
	}
    }

    // getUserPasswd() is called by the CSecurity object when it needs us to
    // read
    // a password from the user.

    // CConnection callback methods

    // serverInit() is called when the serverInit message has been received. At
    // this point we create the desktop window and display it. We also tell the
    // server the pixel format and encodings to use and request the first
    // update.
    public void serverInit() {
	super.serverInit();

	// If using AutoSelect with old servers, start in FullColor
	// mode. See comment in autoSelectFormatAndEncoding.
	if (cp.beforeVersion(3, 8) && autoSelect)
	    fullColour = true;

	serverPF = cp.pf();

	desktop = new DesktopWindow(cp.width, cp.height, serverPF, this);
	VncViewerFx.titleProperty().set(cp.name());

	fullColourPF = desktop.getPreferredPF();

	// Force a switch to the format and encoding we'd like
	formatChange = true;
	encodingChange = true;

	// And kick off the update cycle
	requestNewUpdate();

	// This initial update request is a bit of a corner case, so we need
	// to help out setting the correct format here.
	assert (pendingPFChange);
	desktop.setServerPF(pendingPF);
	cp.setPF(pendingPF);
	pendingPFChange = false;

	Platform.runLater(new Runnable() {
	    @Override
	    public void run() {
		viewer.parent.getChildren().add(desktop);
		desktop.registerEventHandler();
	    }
	});

    }

    // setDesktopSize() is called when the desktop size changes (including when
    // it is set initially).
    public void setDesktopSize(int w, int h) {
	super.setDesktopSize(w, h);
	resizeFramebuffer();
    }

    // setExtendedDesktopSize() is a more advanced version of setDesktopSize()
    public void setExtendedDesktopSize(int reason, int result, int w, int h, ScreenSet layout) {
	super.setExtendedDesktopSize(reason, result, w, h, layout);

	if ((reason == screenTypes.reasonClient) && (result != screenTypes.resultSuccess)) {
	    vlog.error("SetDesktopSize failed: " + result);
	    return;
	}

	resizeFramebuffer();
    }

    // framebufferUpdateStart() is called at the beginning of an update.
    // Here we try to send out a new framebuffer update request so that the
    // next update can be sent out in parallel with us decoding the current
    // one.
    public void framebufferUpdateStart() {
	// Note: This might not be true if sync fences are supported
	pendingUpdate = false;

	requestNewUpdate();
    }

    // framebufferUpdateEnd() is called at the end of an update.
    // For each rectangle, the FdInStream will have timed the speed
    // of the connection, allowing us to select format and encoding
    // appropriately, and then request another incremental update.
    public void framebufferUpdateEnd() {

	desktop.updateWindow();

	if (firstUpdate) {
	    int width, height;

	    // We need fences to make extra update requests and continuous
	    // updates "safe". See fence() for the next step.
	    if (cp.supportsFence)
		writer().writeFence(fenceTypes.fenceFlagRequest | fenceTypes.fenceFlagSyncNext, 0, null);

	    if (cp.supportsSetDesktopSize && viewer.desktopSize.getValue() != null && viewer.desktopSize.getValue().split("x").length == 2) {
		width = Integer.parseInt(viewer.desktopSize.getValue().split("x")[0]);
		height = Integer.parseInt(viewer.desktopSize.getValue().split("x")[1]);
		ScreenSet layout;

		layout = cp.screenLayout;

		if (layout.num_screens() == 0)
		    layout.add_screen(new Screen());
		else if (layout.num_screens() != 1) {

		    while (true) {
			Iterator<Screen> iter = layout.screens.iterator();
			Screen screen = (Screen) iter.next();

			if (!iter.hasNext())
			    break;

			layout.remove_screen(screen.id);
		    }
		}

		Screen screen0 = (Screen) layout.screens.iterator().next();
		screen0.dimensions.tl.x = 0;
		screen0.dimensions.tl.y = 0;
		screen0.dimensions.br.x = width;
		screen0.dimensions.br.y = height;

		writer().writeSetDesktopSize(width, height, layout);
	    }

	    firstUpdate = false;
	}

	// A format change has been scheduled and we are now past the update
	// with the old format. Time to active the new one.
	if (pendingPFChange) {
	    desktop.setServerPF(pendingPF);
	    cp.setPF(pendingPF);
	    pendingPFChange = false;
	}

	// Compute new settings based on updated bandwidth values
	if (autoSelect)
	    autoSelectFormatAndEncoding();
    }

    // The rest of the callbacks are fairly self-explanatory...

    public void setColourMapEntries(int firstColour, int nColours, int[] rgbs) {
	desktop.setColourMapEntries(firstColour, nColours, rgbs);
    }

    // We start timing on beginRect and stop timing on endRect, to
    // avoid skewing the bandwidth estimation as a result of the server
    // being slow or the network having high latency
    public void beginRect(Rect r, int encoding) {
	sock.inStream().startTiming();
	if (encoding != Encodings.encodingCopyRect) {
	    lastServerEncoding = encoding;
	}
    }

    public void endRect(Rect r, int encoding) {
	sock.inStream().stopTiming();
    }

    public void fillRect(Rect r, int p) {
	desktop.fillRect(r.tl.x, r.tl.y, r.width(), r.height(), p);
    }

    public void imageRect(Rect r, Object p) {
	desktop.imageRect(r.tl.x, r.tl.y, r.width(), r.height(), p);
    }

    public void copyRect(Rect r, int sx, int sy) {
	desktop.copyRect(r.tl.x, r.tl.y, r.width(), r.height(), sx, sy);
    }

    public void fence(int flags, int len, byte[] data) {
	// can't call super.super.fence(flags, len, data);
	cp.supportsFence = true;

	if ((flags & fenceTypes.fenceFlagRequest) != 0) {
	    // We handle everything synchronously so we trivially honor these
	    // modes
	    flags = flags & (fenceTypes.fenceFlagBlockBefore | fenceTypes.fenceFlagBlockAfter);

	    writer().writeFence(flags, len, data);
	    return;
	}

	if (len == 0) {
	    // Initial probe
	    if ((flags & fenceTypes.fenceFlagSyncNext) != 0) {
		supportsSyncFence = true;

		if (cp.supportsContinuousUpdates) {
		    vlog.info("Enabling continuous updates");
		    continuousUpdates = true;
		    writer().writeEnableContinuousUpdates(true, 0, 0, cp.width, cp.height);
		}
	    }
	} else {
	    // Pixel format change
	    MemInStream memStream = new MemInStream(data, 0, len);
	    PixelFormat pf = new PixelFormat();

	    pf.read(memStream);

	    desktop.setServerPF(pf);
	    cp.setPF(pf);
	}
    }

    private void resizeFramebuffer() {
	if (desktop == null)
	    return;

	if (continuousUpdates)
	    writer().writeEnableContinuousUpdates(true, 0, 0, cp.width, cp.height);

	if ((cp.width == 0) && (cp.height == 0))
	    return;
	if ((desktop.width() == cp.width) && (desktop.height() == cp.height))
	    return;

	desktop.resize();
    }

    // autoSelectFormatAndEncoding() chooses the format and encoding appropriate
    // to the connection speed:
    //
    // First we wait for at least one second of bandwidth measurement.
    //
    // Above 16Mbps (i.e. LAN), we choose the second highest JPEG quality,
    // which should be perceptually lossless.
    //
    // If the bandwidth is below that, we choose a more lossy JPEG quality.
    //
    // If the bandwidth drops below 256 Kbps, we switch to palette mode.
    //
    // Note: The system here is fairly arbitrary and should be replaced
    // with something more intelligent at the server end.
    //
    private void autoSelectFormatAndEncoding() {
	long kbitsPerSecond = sock.inStream().kbitsPerSecond();
	long timeWaited = sock.inStream().timeWaited();
	boolean newFullColour = fullColour;
	int newQualityLevel = cp.qualityLevel;

	// Always use Tight
	// if (currentEncoding != Encodings.encodingTight) {
	// currentEncoding = Encodings.encodingTight;
	// encodingChange = true;
	// }

	// Check that we have a decent bandwidth measurement
	if ((kbitsPerSecond == 0) || (timeWaited < 100))
	    return;

	// Select appropriate quality level
	if (!cp.noJpeg) {
	    if (kbitsPerSecond > 16000)
		newQualityLevel = 8;
	    else
		newQualityLevel = 6;

	    if (newQualityLevel != cp.qualityLevel) {
		vlog.info("Throughput " + kbitsPerSecond + " kbit/s - changing to quality " + newQualityLevel);
		cp.qualityLevel = newQualityLevel;
		viewer.qualityLevel.setParam(Integer.toString(newQualityLevel));
		encodingChange = true;
	    }
	}

	if (cp.beforeVersion(3, 8)) {
	    // Xvnc from TightVNC 1.2.9 sends out FramebufferUpdates with
	    // cursors "asynchronously". If this happens in the middle of a
	    // pixel format change, the server will encode the cursor with
	    // the old format, but the client will try to decode it
	    // according to the new format. This will lead to a
	    // crash. Therefore, we do not allow automatic format change for
	    // old servers.
	    return;
	}

	// Select best color level
	newFullColour = (kbitsPerSecond > 256);
	if (newFullColour != fullColour) {
	    vlog.info("Throughput " + kbitsPerSecond + " kbit/s - full color is now " + (newFullColour ? "enabled" : "disabled"));
	    fullColour = newFullColour;
	    formatChange = true;
	    forceNonincremental = true;
	}
    }

    // requestNewUpdate() requests an update from the server, having set the
    // format and encoding appropriately.
    private void requestNewUpdate() {
	if (formatChange) {
	    PixelFormat pf;

	    /* Catch incorrect requestNewUpdate calls */
	    assert (!pendingUpdate || supportsSyncFence);

	    if (fullColour) {
		pf = fullColourPF;
	    } else {
		if (lowColourLevel == 0) {
		    pf = verylowColourPF;
		} else if (lowColourLevel == 1) {
		    pf = lowColourPF;
		} else {
		    pf = mediumColourPF;
		}
	    }

	    if (supportsSyncFence) {
		// We let the fence carry the pixel format and switch once we
		// get the response back. That way we will be synchronised with
		// when the server switches.
		MemOutStream memStream = new MemOutStream();

		pf.write(memStream);

		writer().writeFence(fenceTypes.fenceFlagRequest | fenceTypes.fenceFlagSyncNext, memStream.length(), (byte[]) memStream.data());
	    } else {
		// New requests are sent out at the start of processing the last
		// one, so we cannot switch our internal format right now (doing
		// so
		// would mean misdecoding the current update).
		pendingPFChange = true;
		pendingPF = pf;
	    }

	    String str = pf.print();
	    vlog.info("Using pixel format " + str);
	    writer().writeSetPixelFormat(pf);

	    formatChange = false;
	}

	checkEncodings();

	if (forceNonincremental || !continuousUpdates) {
	    pendingUpdate = true;
	    writer().writeFramebufferUpdateRequest(new Rect(0, 0, cp.width, cp.height), !forceNonincremental);
	}

	forceNonincremental = false;
    }

    // //////////////////////////////////////////////////////////////////
    // The following methods are all called from the GUI thread

    // close() shuts down the socket, thus waking up the RFB thread.
    public void close() {
	deleteWindow();
	shuttingDown = true;
	try {
	    if (sock != null)
		sock.shutdown();
	} catch (java.lang.Exception e) {
	    throw new Exception(e.getMessage());
	}
    }

    public void refresh() {
	writer().writeFramebufferUpdateRequest(new Rect(0, 0, cp.width, cp.height), false);
	pendingUpdate = true;
    }

    public void toggleFullScreen() {
	fullScreen = !fullScreen;
	viewer.setFullScreen(fullScreen);
    }

    // writeClientCutText() is called from the clipboard dialog
    public void writeClientCutText(String str, int len) {
	if (state() != RFBSTATE_NORMAL || shuttingDown)
	    return;
	writer().writeClientCutText(str, len);
    }

    
    @Override
    public void fireInputEvent(InputEventMessage ev) {
	if (ev instanceof KeyEventMessage) {
	    KeyEventMessage k = (KeyEventMessage) ev;
	    writer().writeKeyEvent(k.getKey(), k.isDown());
	    return;
	}

	if (ev instanceof PointerEventMessage) {
	    PointerEventMessage p = (PointerEventMessage) ev;
	    writer().writePointerEvent(new Point(p.getxPos(), p.getyPos()), p.getButtonMask());
	    return;
	}
    }

    // //////////////////////////////////////////////////////////////////
    // The following methods are called from both RFB and GUI threads

    // checkEncodings() sends a setEncodings message if one is needed.
    private void checkEncodings() {
	if (encodingChange && (writer() != null)) {
	    vlog.info("Requesting " + Encodings.encodingName(currentEncoding) + " encoding");
	    writer().writeSetEncodings(currentEncoding, true);
	    encodingChange = false;
	}
    }

    @Override
    public boolean getUserPasswd(StringBuffer user, StringBuffer password) {
	String PlainPasswd = viewer.passwd.getValueStr();
	password.append(PlainPasswd);
	password.setLength(PlainPasswd.length());
	return true;
    }

}
