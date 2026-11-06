package io.mosip.registration.controller;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.*;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import io.mosip.registration.JavaFxTest;
import javafx.event.EventType;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Regression tests for mapped physical keyboard events and virtual keyboard isolation. */
public class VirtualKeyboardTest extends JavaFxTest {
	private static final String ARABIC_A = "\u0634";
	private static final String SHIFTED_A = "\u0650";
	private static class FocusableField extends TextField {
		void focus(boolean focused) {
			setFocused(focused);
		}
	}

	private FocusableField field;
	private VirtualKeyboard keyboard;
	private Scene scene;

	@Before
	public void setUp() throws Exception {
		onFxThread(() -> {
			field = new FocusableField();
			keyboard = new VirtualKeyboard("ara");
			scene = new Scene(new VBox(field));
			scene.getRoot().applyCss();
			keyboard.changeControlOfKeyboard(field);
		});
	}

	/** Checks repeated Space/letter rollover with both key-release orders. */
	@Test
	public void should_preserveSpaceAndMappedLetter_when_keysOverlapInEitherReleaseOrder() throws Exception {
		onFxThread(() -> {
			String expected = "";
			for (int i = 0; i < 6; i++) {
				press(SPACE);
				press(A);
				type(" ");
				type("a");
				release(i % 2 == 0 ? A : SPACE);
				release(i % 2 == 0 ? SPACE : A);
				expected += " " + ARABIC_A;
				assertEquals(expected, field.getText());
			}
		});
	}

	/** Ensures a released key cannot leave a stale mapping behind another press. */
	@Test
	public void should_discardUntypedKey_when_releasedBehindQueueHead() throws Exception {
		onFxThread(() -> {
			press(B);
			press(A);
			release(A);
			type("b");
			release(B);
			type("x");
			assertEquals("\u0644\u0627x", field.getText());
		});
	}

	/** Ensures navigation and editing keys do not consume printable mappings. */
	@Test
	public void should_preserveNextMappedLetter_when_navigationKeyIsHeld() throws Exception {
		onFxThread(() -> {
			field.setText("xy");
			field.end();
			press(LEFT);
			press(A);
			type("a");
			release(A);
			release(LEFT);
			assertEquals("x" + ARABIC_A + "y", field.getText());
			press(BACK_SPACE);
			release(BACK_SPACE);
			assertEquals("xy", field.getText());
		});
	}

	/** Covers platforms that do or do not emit a typed Backspace control character. */
	@Test
	public void should_preserveLetterMapping_when_editingKeyEmitsOptionalTypedEvent() throws Exception {
		onFxThread(() -> {
			for (boolean emitsTypedEvent : new boolean[] { true, false }) {
				field.setText("x");
				field.end();
				press(BACK_SPACE);
				press(A);
				if (emitsTypedEvent) {
					type("\b");
				}
				type("a");
				release(A);
				release(BACK_SPACE);
				assertEquals(ARABIC_A, field.getText());
			}
		});
	}

	/** Checks that focus transitions discard pending physical key mappings. */
	@Test
	public void should_clearPendingMapping_when_focusIsLost() throws Exception {
		onFxThread(() -> {
			field.focus(true);
			press(A);
			field.focus(false);
			field.focus(true);
			type("x");
			assertEquals("x", field.getText());
		});
	}

	/** Covers Ctrl selection and Shift/Caps Lock mappings together. */
	@Test
	public void should_preserveSelectionAndMappings_when_usingShortcutShiftAndCapsLock() throws Exception {
		onFxThread(() -> {
			field.setText("replace me");
			fire(KEY_PRESSED, A, "", false, true);
			assertEquals("replace me", field.getSelectedText());
			fire(KEY_RELEASED, A, "", false, true);
			fire(KEY_PRESSED, A, "", true, false);
			fire(KEY_TYPED, UNDEFINED, "A", true, false);
			release(A);
			press(CAPS);
			release(CAPS);
			press(A);
			type("A");
			release(A);
			assertEquals(SHIFTED_A + SHIFTED_A, field.getText());
		});
	}

	/** Checks virtual keyboard clicks do not steal pending physical mappings. */
	@Test
	public void should_preservePhysicalMapping_when_virtualButtonIsPressed() throws Exception {
		onFxThread(() -> {
			Stage stage = new Stage();
			try {
				stage.setScene(scene);
				field.requestFocus();
				keyboard.setParentStage(stage);
				Button button = keyboard.view().lookupAll(".button").stream()
						.map(Button.class::cast).filter(key -> ARABIC_A.equals(key.getText()))
						.findFirst().orElseThrow();
				press(A);
				button.fire();
				type("a");
				release(A);
				assertEquals(ARABIC_A + ARABIC_A, field.getText());
			} finally {
				stage.close();
			}
		});
	}

	private void press(KeyCode code) {
		fire(KEY_PRESSED, code, "", false, false);
	}

	private void release(KeyCode code) {
		fire(KEY_RELEASED, code, "", false, false);
	}

	private void type(String character) {
		fire(KEY_TYPED, UNDEFINED, character, false, false);
	}

	private void fire(EventType<KeyEvent> type, KeyCode code, String character, boolean shift, boolean ctrl) {
		field.fireEvent(new KeyEvent(type, character, code.getName(), code, shift, ctrl, false, false));
	}
}
