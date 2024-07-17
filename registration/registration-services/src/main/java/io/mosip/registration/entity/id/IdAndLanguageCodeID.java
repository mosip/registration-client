package io.mosip.registration.entity.id;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite key for IdAndLanguageCodeID.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdAndLanguageCodeID implements Serializable {
	/**
	* 
	*/
	private static final long serialVersionUID = 1L;
	
	@Column(name = "id", nullable = false)
	private String id;

	@Column(name = "lang_code", nullable = false, length = 3)
	private String langCode;

}
