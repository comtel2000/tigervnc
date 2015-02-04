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
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import org.jfxvnc.input.InputEventListener;
import org.jfxvnc.input.PointerEventMessage;

public class PointerEventHandler implements IKeysyms {

    private InputEventListener listener;

    private final BooleanProperty enabledProperty = new SimpleBooleanProperty(false);
    private final EventHandler<MouseEvent> mouseEventHandler;
    private final EventHandler<ScrollEvent> scrollEventHandler;

    public PointerEventHandler() {
	this.mouseEventHandler = (e) -> {
	    if (enabledProperty.get()) {
		sendMouseEvents(e);
		e.consume();
	    }
	};
	this.scrollEventHandler = (e) -> {
	    if (enabledProperty.get()) {
		sendScrollEvents(e);
		e.consume();
	    }
	};
    }

    public void setInputEventListener(InputEventListener listener){
	this.listener = listener;
    }
    
    public BooleanProperty enabledProperty() {
	return enabledProperty;
    }

    public void register(Node node) {

	node.addEventFilter(ScrollEvent.SCROLL, scrollEventHandler);

	node.addEventFilter(MouseEvent.MOUSE_PRESSED, mouseEventHandler);
	node.addEventFilter(MouseEvent.MOUSE_MOVED, mouseEventHandler);
	node.addEventFilter(MouseEvent.MOUSE_DRAGGED, mouseEventHandler);
	node.addEventFilter(MouseEvent.MOUSE_RELEASED, mouseEventHandler);

    }

    public void unregister(Node node) {

	node.removeEventFilter(ScrollEvent.SCROLL, scrollEventHandler);

	node.removeEventFilter(MouseEvent.MOUSE_PRESSED, mouseEventHandler);
	node.removeEventFilter(MouseEvent.MOUSE_MOVED, mouseEventHandler);
	node.removeEventFilter(MouseEvent.MOUSE_DRAGGED, mouseEventHandler);
	node.removeEventFilter(MouseEvent.MOUSE_RELEASED, mouseEventHandler);

    }

    private void sendMouseEvents(MouseEvent event) {
	byte buttonMask = 0;
	if (event.getEventType() == MouseEvent.MOUSE_PRESSED || event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
	    if (event.isMiddleButtonDown()) {
		buttonMask = 2;
	    } else if (event.isSecondaryButtonDown()) {
		buttonMask = 4;
	    } else {
		buttonMask = 1;
	    }
	    fire(new PointerEventMessage(buttonMask, (int) event.getX(), (int) event.getY()));
	} else if (event.getEventType() == MouseEvent.MOUSE_RELEASED || event.getEventType() == MouseEvent.MOUSE_MOVED) {
	    buttonMask = 0;
	}

	fire(new PointerEventMessage(buttonMask, (int) event.getX(), (int) event.getY()));

    }

    private void sendScrollEvents(ScrollEvent event) {
	fire(new PointerEventMessage(event.getDeltaY() < 0 ? (byte) 8 : (byte) 16, (int) event.getX(), (int) event.getY()));
    }

    private synchronized void fire(PointerEventMessage msg) {
	if (listener != null){
	    listener.fireInputEvent(msg);
	}
    }

}
