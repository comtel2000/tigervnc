/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * Copyright (C) 2011-2012 Brian P.Hinz
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
package org.jfxvnc;

import java.nio.ByteOrder;

import javafx.scene.image.WritableImage;

import com.tigervnc.rfb.LogWriter;
import com.tigervnc.rfb.PixelBuffer;
import com.tigervnc.rfb.PixelFormat;

abstract public class PlatformPixelBuffer extends PixelBuffer {

    protected WritableImage image;

    int nColours;
    byte[] reds;
    byte[] greens;
    byte[] blues;

    CConn cc;
    DesktopWindow desktop;

    static LogWriter vlog = new LogWriter("PlatformPixelBuffer");

    public PlatformPixelBuffer(int w, int h, CConn cc_, DesktopWindow desktop_) {
	cc = cc_;
	desktop = desktop_;
	PixelFormat nativePF = getNativePF();
	if (nativePF.depth > cc.serverPF.depth) {
	    setPF(cc.serverPF);
	} else {
	    setPF(nativePF);
	}
	resize(w, h);
    }

    abstract public void resize(int w, int h);

    public PixelFormat getNativePF() {
	//RGB888
	PixelFormat pf;
	int depth = 24;
	int bpp = (depth > 16 ? 32 : (depth > 8 ? 16 : 8));
	ByteOrder byteOrder = ByteOrder.nativeOrder();
	boolean bigEndian = (byteOrder == ByteOrder.BIG_ENDIAN ? true : false);
	boolean trueColour = (depth > 8 ? true : false);
	int redShift = 16;
	int greenShift = 8;
	int blueShift = 0;
	pf = new PixelFormat(bpp, depth, bigEndian, trueColour, (depth > 8 ? 0xff : 0), (depth > 8 ? 0xff : 0), (depth > 8 ? 0xff : 0), (depth > 8 ? redShift : 0),
		(depth > 8 ? greenShift : 0), (depth > 8 ? blueShift : 0));

	vlog.info("Native pixel format is " + pf.print());
	return pf;
    }

    abstract public void imageRect(int x, int y, int w, int h, Object pix);

    // setColourMapEntries() changes some of the entries in the colourmap.
    // However these settings won't take effect until updateColourMap() is
    // called. This is because getting java to recalculate its internal
    // translation table and redraw the screen is expensive.

    public void setColourMapEntries(int firstColour, int nColours_, int[] rgbs) {
	nColours = nColours_;
	reds = new byte[nColours];
	blues = new byte[nColours];
	greens = new byte[nColours];
	for (int i = 0; i < nColours; i++) {
	    reds[firstColour + i] = (byte) (rgbs[i * 3] >> 8);
	    greens[firstColour + i] = (byte) (rgbs[i * 3 + 1] >> 8);
	    blues[firstColour + i] = (byte) (rgbs[i * 3 + 2] >> 8);
	}
    }

    public void updateColourMap() {
	vlog.error("not supported yet");
    }
}
