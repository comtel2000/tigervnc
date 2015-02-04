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
package org.jfxvnc.input;

public class PointerEventMessage implements InputEventMessage {

    private final byte buttonMask;

    private final int xPos;
    private final int yPos;

    public PointerEventMessage(byte buttonMask, int xPos, int yPos) {
	super();
	this.buttonMask = buttonMask;
	this.xPos = xPos;
	this.yPos = yPos;
    }

    public byte getButtonMask() {
	return buttonMask;
    }

    public int getxPos() {
	return xPos;
    }

    public int getyPos() {
	return yPos;
    }

    @Override
    public String toString() {
	return "PointerEventMessage [buttonMask=" + buttonMask + ", xPos=" + xPos + ", yPos=" + yPos + "]";
    }

}
