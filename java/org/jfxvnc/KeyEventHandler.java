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
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jfxvnc.input.InputEventListener;
import org.jfxvnc.input.KeyEventMessage;

public class KeyEventHandler implements IKeysyms {

    private InputEventListener listener;

    private final BooleanProperty enabledProperty = new SimpleBooleanProperty(false);
    private final EventHandler<KeyEvent> keyEventHandler;

    private boolean lastCodePointRelease;
    private int lastCodePoint;

    private boolean SHIFT_KEY_DOWN = false;
    private boolean CTRL_KEY_DOWN = false;
    private boolean META_KEY_DOWN = false;
    private boolean ALT_KEY_DOWN = false;

    public KeyEventHandler() {
	this.keyEventHandler = (e) -> {
	    if (enabledProperty.get()) {
		sendKeyEvents(e);
		e.consume();
	    }
	};
    }

    public void setInputEventListener(InputEventListener listener) {
	this.listener = listener;
    }

    public BooleanProperty enabledProperty() {
	return enabledProperty;
    }

    public void register(Scene scene) {
	resetModifier();
	scene.addEventFilter(KeyEvent.KEY_PRESSED, keyEventHandler);
	scene.addEventFilter(KeyEvent.KEY_TYPED, keyEventHandler);
	scene.addEventFilter(KeyEvent.KEY_RELEASED, keyEventHandler);
    }

    public void unregister(Scene scene) {
	scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyEventHandler);
	scene.removeEventFilter(KeyEvent.KEY_TYPED, keyEventHandler);
	scene.removeEventFilter(KeyEvent.KEY_RELEASED, keyEventHandler);
    }

    private static boolean isModifierPressed(KeyEvent event) {
	return event.isAltDown() || event.isControlDown() || event.isMetaDown() || event.isShortcutDown();
    }

    private void resetModifier() {
	SHIFT_KEY_DOWN = false;
	CTRL_KEY_DOWN = false;
	META_KEY_DOWN = false;
	ALT_KEY_DOWN = false;
    }

    public void sendKeyEvents(KeyEvent event) {
	if (event.isConsumed()) {
	    return;
	}
	if (event.getEventType() == KeyEvent.KEY_TYPED) {
	    if (!isModifierPressed(event) && event.getCode() == KeyCode.UNDEFINED) {
		int codePoint = event.getCharacter().codePointAt(0);
		lastCodePoint = codePoint;
		lastCodePointRelease = true;
		fire(new KeyEventMessage(true, codePoint));
	    }
	    return;
	}

	if (event.getCode().isFunctionKey()) {
	    sendFunctionKeyEvents(event, event.getEventType() == KeyEvent.KEY_PRESSED);
	    return;
	}
	if (event.getCode().isModifierKey()) {
	    sendModifierKeyEvents(event, event.getEventType() == KeyEvent.KEY_PRESSED);
	    return;
	}
	if (event.getCode().isNavigationKey()) {
	    sendNavigationKeyEvents(event, event.getEventType() == KeyEvent.KEY_PRESSED);
	    return;
	}

	if (sendSpecialKeyEvents(event, event.getEventType() == KeyEvent.KEY_PRESSED)) {
	    return;
	}

	if (event.isShortcutDown() || event.isControlDown()) {
	    int codePoint = event.getText().codePointAt(0);
	    fire(new KeyEventMessage(event.getEventType() == KeyEvent.KEY_PRESSED, codePoint));
	    return;
	}

	if (event.getEventType() == KeyEvent.KEY_RELEASED) {
	    if (lastCodePointRelease) {
		lastCodePointRelease = false;
		fire(new KeyEventMessage(false, lastCodePoint));
	    } else {
		int codePoint = event.getText().codePointAt(0);
		fire(new KeyEventMessage(false, codePoint));
	    }
	    return;
	}

    }

    private boolean sendSpecialKeyEvents(KeyEvent event, boolean isDown) {
	switch (event.getCode()) {
	case PRINTSCREEN:
	    fire(new KeyEventMessage(isDown, RFB_Print));
	    return true;
	case INSERT:
	    fire(new KeyEventMessage(isDown, RFB_Insert));
	    return true;
	case UNDO:
	    fire(new KeyEventMessage(isDown, RFB_Undo));
	    return true;
	case AGAIN:
	    fire(new KeyEventMessage(isDown, RFB_Redo));
	    return true;
	case FIND:
	    fire(new KeyEventMessage(isDown, RFB_Find));
	    return true;
	case CANCEL:
	    fire(new KeyEventMessage(isDown, RFB_Cancel));
	    return true;
	case HELP:
	    fire(new KeyEventMessage(isDown, RFB_Help));
	    return true;
	case STOP:
	    fire(new KeyEventMessage(isDown, RFB_Break));
	    return true;
	case MODECHANGE:
	    fire(new KeyEventMessage(isDown, RFB_Mode_switch));
	    return true;
	case NUM_LOCK:
	    fire(new KeyEventMessage(isDown, RFB_Num_Lock));
	    return true;
	case BACK_SPACE:
	    fire(new KeyEventMessage(isDown, RFB_BackSpace));
	    return true;
	case TAB:
	    fire(new KeyEventMessage(isDown, RFB_Tab));
	    return true;
	case CLEAR:
	    fire(new KeyEventMessage(isDown, RFB_Clear));
	    return true;
	case ENTER:
	    fire(new KeyEventMessage(isDown, RFB_Return));
	    return true;
	case PAUSE:
	    fire(new KeyEventMessage(isDown, RFB_Pause));
	    return true;
	case SCROLL_LOCK:
	    fire(new KeyEventMessage(isDown, RFB_Scroll_Lock));
	    return true;
	case ESCAPE:
	    fire(new KeyEventMessage(isDown, RFB_Escape));
	    return true;
	case DELETE:
	    fire(new KeyEventMessage(isDown, RFB_Delete));
	    return true;
	case SPACE:
	    fire(new KeyEventMessage(isDown, RFB_space));
	    return true;
	case CAPS:
	    fire(new KeyEventMessage(isDown, RFB_Caps_Lock));
	    return true;
	case CHANNEL_DOWN:
	    fire(new KeyEventMessage(isDown, RFB_N));
	    return true;
	case NUMPAD0:
	    fire(new KeyEventMessage(isDown, RFB_KP_0));
	    return true;
	case NUMPAD1:
	    fire(new KeyEventMessage(isDown, RFB_KP_1));
	    return true;
	case NUMPAD2:
	    fire(new KeyEventMessage(isDown, RFB_KP_2));
	    return true;
	case NUMPAD3:
	    fire(new KeyEventMessage(isDown, RFB_KP_3));
	    return true;
	case NUMPAD4:
	    fire(new KeyEventMessage(isDown, RFB_KP_4));
	    return true;
	case NUMPAD5:
	    fire(new KeyEventMessage(isDown, RFB_KP_5));
	    return true;
	case NUMPAD6:
	    fire(new KeyEventMessage(isDown, RFB_KP_6));
	    return true;
	case NUMPAD7:
	    fire(new KeyEventMessage(isDown, RFB_KP_7));
	    return true;
	case NUMPAD8:
	    fire(new KeyEventMessage(isDown, RFB_KP_8));
	    return true;
	case NUMPAD9:
	    fire(new KeyEventMessage(isDown, RFB_KP_9));
	    return true;
	default:
	    return false;
	}
    }

    private void sendNavigationKeyEvents(KeyEvent event, boolean isDown) {
	switch (event.getCode()) {
	case HOME:
	    fire(new KeyEventMessage(isDown, RFB_Home));
	    break;
	case KP_UP:
	    fire(new KeyEventMessage(isDown, RFB_KP_Up));
	    break;
	case KP_RIGHT:
	    fire(new KeyEventMessage(isDown, RFB_KP_Right));
	    break;
	case KP_DOWN:
	    fire(new KeyEventMessage(isDown, RFB_KP_Down));
	    break;
	case KP_LEFT:
	    fire(new KeyEventMessage(isDown, RFB_KP_Left));
	    break;
	case UP:
	    fire(new KeyEventMessage(isDown, RFB_Up));
	    break;
	case RIGHT:
	    fire(new KeyEventMessage(isDown, RFB_Right));
	    break;
	case DOWN:
	    fire(new KeyEventMessage(isDown, RFB_Down));
	    break;
	case LEFT:
	    fire(new KeyEventMessage(isDown, RFB_Left));
	    break;
	case TRACK_PREV:
	    fire(new KeyEventMessage(isDown, RFB_PreviousCandidate));
	    break;
	case PAGE_UP:
	    fire(new KeyEventMessage(isDown, RFB_Page_Up));
	    break;
	case TRACK_NEXT:
	    fire(new KeyEventMessage(isDown, RFB_Next));
	    break;
	case PAGE_DOWN:
	    fire(new KeyEventMessage(isDown, RFB_Page_Down));
	    break;
	case END:
	    fire(new KeyEventMessage(isDown, RFB_End));
	    break;
	case BEGIN:
	    fire(new KeyEventMessage(isDown, RFB_Begin));
	    break;
	default:
	    break;
	}
    }

    private void sendFunctionKeyEvents(KeyEvent event, boolean isDown) {
	switch (event.getCode()) {
	case F1:
	    fire(new KeyEventMessage(isDown, RFB_F1));
	    break;
	case F2:
	    fire(new KeyEventMessage(isDown, RFB_F2));
	    break;
	case F3:
	    fire(new KeyEventMessage(isDown, RFB_F3));
	    break;
	case F4:
	    fire(new KeyEventMessage(isDown, RFB_F4));
	    break;
	case F5:
	    fire(new KeyEventMessage(isDown, RFB_F5));
	    break;
	case F6:
	    fire(new KeyEventMessage(isDown, RFB_F6));
	    break;
	case F7:
	    fire(new KeyEventMessage(isDown, RFB_F7));
	    break;
	case F8:
	    fire(new KeyEventMessage(isDown, RFB_F8));
	    break;
	case F9:
	    fire(new KeyEventMessage(isDown, RFB_F9));
	    break;
	case F10:
	    fire(new KeyEventMessage(isDown, RFB_F10));
	    break;
	case F11:
	    fire(new KeyEventMessage(isDown, RFB_F11));
	    break;
	case F12:
	    fire(new KeyEventMessage(isDown, RFB_F12));
	    break;
	default:
	    break;
	}
    }

    private void sendModifierKeyEvents(KeyEvent event, boolean isDown) {
	switch (event.getCode()) {
	case SHIFT:
	    fire(new KeyEventMessage(SHIFT_KEY_DOWN = isDown, RFB_Shift_L));
	    break;
	case CONTROL:
	    fire(new KeyEventMessage(CTRL_KEY_DOWN = isDown, RFB_Control_L));
	    break;
	case META:
	    fire(new KeyEventMessage(META_KEY_DOWN = isDown, RFB_Meta_L));
	    break;
	case ALT:
	    fire(new KeyEventMessage(ALT_KEY_DOWN = isDown, RFB_Alt_L));
	    break;
	case ALT_GRAPH:
	    fire(new KeyEventMessage(ALT_KEY_DOWN = isDown, RFB_Alt_R));
	    break;
	default:
	    break;
	}
    }

    private void sendModifierEvents(KeyEvent event) {
	if (!SHIFT_KEY_DOWN && event.isShiftDown()) {
	    fire(new KeyEventMessage(SHIFT_KEY_DOWN = true, RFB_Shift_L));
	}
	if (!CTRL_KEY_DOWN && event.isControlDown()) {
	    fire(new KeyEventMessage(CTRL_KEY_DOWN = true, RFB_Control_L));
	}
	if (!META_KEY_DOWN && event.isMetaDown()) {
	    fire(new KeyEventMessage(META_KEY_DOWN = true, RFB_Meta_L));
	}
	if (!ALT_KEY_DOWN && event.isAltDown()) {
	    fire(new KeyEventMessage(ALT_KEY_DOWN = true, RFB_Alt_L));
	}
    }

    private void releaseModifier() {
	if (SHIFT_KEY_DOWN) {
	    fire(new KeyEventMessage(SHIFT_KEY_DOWN = false, RFB_Shift_L));
	}
	if (CTRL_KEY_DOWN) {
	    fire(new KeyEventMessage(CTRL_KEY_DOWN = false, RFB_Control_L));
	}
	if (META_KEY_DOWN) {
	    fire(new KeyEventMessage(META_KEY_DOWN = false, RFB_Meta_L));
	}
	if (ALT_KEY_DOWN) {
	    fire(new KeyEventMessage(ALT_KEY_DOWN = false, RFB_Alt_L));
	}
    }

    private synchronized void fire(KeyEventMessage msg) {
	if (listener != null) {
	    listener.fireInputEvent(msg);
	}
    }

}
