package io.mosip.registration.controller.settings;

import org.springframework.stereotype.Component;

import javafx.scene.layout.HBox;

@Component
public interface SettingsInterface {
	
	public void setHeaderLabel(String headerLabel);
	
	public HBox getShortCut(String shortcutIcon);

}
