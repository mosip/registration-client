package io.mosip.registration.util.control.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.springframework.context.ApplicationContext;

import io.mosip.kernel.core.transliteration.spi.Transliteration;
import io.mosip.registration.JavaFxTest;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.util.common.DemographicChangeActionHandler;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/** Verifies immediate demographic updates and the delayed dependent-field refresh. */
public class TextFieldFxControlTest extends JavaFxTest {
	/** Ensures successive edits coalesce into one refresh without postponing validation or updates. */
	@Test
	public void should_refreshOnce_when_multipleEditsUpdateDemographicsImmediately() throws Exception {
		CountDownLatch refreshed = new CountDownLatch(1);
		AtomicInteger refreshCount = new AtomicInteger();
		AtomicInteger validations = new AtomicInteger();
		AtomicInteger actions = new AtomicInteger();
		RegistrationDTO data = new RegistrationDTO();
		data.setSelectedLanguagesByApplicant(List.of("eng", "fra"));
		onFxThread(() -> {
			ApplicationContext context = mock(ApplicationContext.class);
			Transliteration<String> transliteration = (from, to, value) -> to + "-" + value;
			when(context.getBeansOfType(Transliteration.class)).thenReturn(Map.of("test", transliteration));
			when(context.getBean(DemographicChangeActionHandler.class)).thenReturn(new DemographicChangeActionHandler() {
				@Override
				public void actionHandle(Pane parent, String sourceId, String action) {
					assertEquals("nameeng", sourceId);
					assertEquals("onNameChanged", action);
					actions.incrementAndGet();
				}
			});

			TextField primary = new TextField();
			primary.setId("nameeng");
			TextField secondary = new TextField();
			secondary.setId("namefra");
			UiFieldDTO name = new UiFieldDTO();
			name.setId("name");
			name.setType("string");
			name.setTransliterate(true);
			name.setChangeAction("onNameChanged");

			TextFieldFxControl dependent = new TextFieldFxControl(context) {
				{
					uiFieldDTO = new UiFieldDTO();
					uiFieldDTO.setId("dependent");
					uiFieldDTO.setType("string");
				}

				@Override
				protected boolean isFieldVisible(UiFieldDTO schema) {
					return !"abc".equals(data.getDemographics().get("name"));
				}

				@Override
				protected RegistrationDTO getRegistrationDTo() {
					return data;
				}
			};
			Label dependentNode = new Label();
			dependent.setNode(dependentNode);
			TextFieldFxControl source = new TextFieldFxControl(context) {
				{ uiFieldDTO = name; }

				@Override
				protected RegistrationDTO getRegistrationDTo() {
					return data;
				}

				@Override
				public boolean isValid() {
					validations.incrementAndGet();
					return !primary.getText().equals("!");
				}

				@Override
				public void refreshFields() {
					refreshCount.incrementAndGet();
					dependent.refresh();
					refreshed.countDown();
				}
			};
			Label title = new Label();
			title.setId("nameLabel");
			Label message = new Label();
			message.setId("nameengMessage");
			source.setNode(new VBox(primary, secondary, title, message));
			source.setListener(primary);
			for (String text : List.of("a", "!", "ab", "abc")) {
				primary.setText(text);
				assertEquals("fra-" + text, secondary.getText());
				assertEquals(text.equals("!") ? null : text, data.getDemographics().get("name"));
			}
			assertEquals(4, validations.get());
			assertEquals(3, actions.get());
			assertEquals(0, refreshCount.get());
			assertTrue(dependentNode.isVisible());
			assertTrue(dependentNode.isManaged());
			visibleAfterRefresh = dependentNode;
		});
		assertTrue("Dependent field refresh timed out", refreshed.await(5, TimeUnit.SECONDS));
		onFxThread(() -> {
			assertEquals(1, refreshCount.get());
			assertFalse(visibleAfterRefresh.isVisible());
			assertFalse(visibleAfterRefresh.isManaged());
		});
	}

	private Label visibleAfterRefresh;
}
