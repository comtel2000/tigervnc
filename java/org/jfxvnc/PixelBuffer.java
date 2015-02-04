/* Copyright (C) 2012 Brian P. Hinz
 * Copyright (C) 2012 D. R. Commander.  All Rights Reserved.
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

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import com.tigervnc.rfb.LogWriter;

public class PixelBuffer extends PlatformPixelBuffer {

    static LogWriter vlog = new LogWriter("PixelBuffer");

    public PixelBuffer(int w, int h, CConn cc_, DesktopWindow desktop_) {
	super(w, h, cc_, desktop_);

    }

    public void setPF(com.tigervnc.rfb.PixelFormat pf) {
	super.setPF(pf);
	createCanvas(width(), height());
    }

    public void updateColourMap() {
	super.updateColourMap();
	createCanvas(width_, height_);
    }

    // resize() resizes the image, preserving the image data where possible.
    public void resize(int w, int h) {
	if (w == width() && h == height()) {
	    return;
	}
	width_ = w;
	height_ = h;
	createCanvas(w, h);
	cc.viewer.setSize(w, h);
    }

    private void createCanvas(int w, int h) {
	if (w == 0 || h == 0) {
	    return;
	}
	image = new WritableImage(w, h);
	desktop.setCanvas(image);
    }

    public void fillRect(int x, int y, int w, int h, int pix) {
	int[] fill;
	switch (format.depth) {
	case 24:
	    fill = new int[w * h];
	    Arrays.fill(fill, pix);
	    image.getPixelWriter().setPixels(x, y, w, h, PixelFormat.getIntArgbInstance(), fill, 0, w);

	    break;
	default:
	    //TODO: convert to argb
	    fill = new int[w * h];
	    Arrays.fill(fill, pix);
	    image.getPixelWriter().setPixels(x, y, w, h, PixelFormat.getIntArgbInstance(), fill, 0, w);
	    break;
	}
    }

    public void imageRect(int x, int y, int w, int h, Object pix) {
	if (pix instanceof byte[]) {
	    byte[] buf = (byte[]) pix;
	    // decode Tight jpg image
	    Image img = new Image(new ByteArrayInputStream(buf));
	    PixelReader reader = img.getPixelReader();
	    image.getPixelWriter().setPixels(x, y, w, h, reader, 0, 0);

	} else {
	    int[] buf = (int[]) pix;
	    image.getPixelWriter().setPixels(x, y, w, h, PixelFormat.getIntArgbInstance(), buf, 0, w);
	}
    }

    public void copyRect(int x, int y, int w, int h, int srcX, int srcY) {

	PixelReader reader = image.getPixelReader();
	WritableImage copyRect = new WritableImage(w, h);
	copyRect.getPixelWriter().setPixels(0, 0, w, h, reader, srcX, srcY);

	image.getPixelWriter().setPixels(x, y, w, h, copyRect.getPixelReader(), 0, 0);

    }

}
