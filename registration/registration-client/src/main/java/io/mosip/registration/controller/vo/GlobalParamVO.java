package io.mosip.registration.controller.vo;

import javafx.beans.property.SimpleStringProperty;

public class GlobalParamVO {
	
	private SimpleStringProperty key;
	private SimpleStringProperty serverValue;
	private SimpleStringProperty localValue;
	
	public String getKey() {
		return key.get();
	}

	public void setKey(String key) {
		this.key = new SimpleStringProperty(key);
	}
	
	public String getServerValue() {
		return serverValue.get();
	}

	public void setServerValue(String serverValue) {
		this.serverValue = new SimpleStringProperty(serverValue);
	}
	
	public String getLocalValue() {
		return localValue.get();
	}

	public void setLocalValue(String localValue) {
		this.localValue = new SimpleStringProperty(localValue);
	}

}
