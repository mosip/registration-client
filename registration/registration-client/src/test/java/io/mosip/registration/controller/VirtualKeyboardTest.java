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

	@Test
	public void spaceAndMappedLetterRolloverInBothReleaseOrders() throws Exception {
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

	@Test
	public void releaseDiscardsAnUntypedKeyBehindTheQueueHead() throws Exception {
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

	@Test
	public void heldNavigationKeyDoesNotTakeTheNextLetter() throws Exception {
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

	@Test
	public void editingKeyWithOrWithoutTypedControlCharacterPreservesNextMapping() throws Exception {
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

	@Test
	public void focusLossDiscardsPendingMapping() throws Exception {
		onFxThread(() -> {
			field.focus(true);
			press(A);
			field.focus(false);
			field.focus(true);
			type("x");
			assertEquals("x", field.getText());
		});
	}

	@Test
	public void shortcutsSelectionShiftAndCapsLock() throws Exception {
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

	@Test
	public void virtualButtonDoesNotTakePendingPhysicalMapping() throws Exception {
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
