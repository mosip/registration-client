package io.mosip.registration.dto.packetmanager;

import lombok.Data;

@Data
public class DocumentDto {

	private byte[] document;
	private String value;
	private String type;
	private String category;
	private String owner;
	private String format;
	private String refNumber;

	public void setValue(String value) {
		this.value = value == null || value.isBlank() ? null : value.trim();
	}

	public void setType(String type) {
		this.type = type == null || type.isBlank() ? null : type.trim();
	}

	public void setCategory(String category) {
		this.category = category == null || category.isBlank() ? null : category.trim();
	}

	public void setOwner(String owner) {
		this.owner = owner == null || owner.isBlank() ? null : owner.trim();
	}

	public void setFormat(String format) {
		this.format = format == null || format.isBlank() ? null : format.trim();
	}

	public void setRefNumber(String refNumber) {
		this.refNumber = refNumber == null || refNumber.isBlank() ? null : refNumber.trim();
	}


}
