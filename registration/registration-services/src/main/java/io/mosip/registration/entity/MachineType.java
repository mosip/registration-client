package io.mosip.registration.entity;

import java.io.Serializable;

import javax.persistence.*;

import io.mosip.registration.entity.id.CodeAndLanguageCodeID;

/**
 * This Entity Class contains list of machine types[Desktop,Laptop...] with respect to language code.
 * The data for this table will come through sync from server master table.
 *
 * @author Sreekar chukka
 * @since 1.0.0
 */
@Entity
@Table(name = "machine_type", schema = "reg")
public class MachineType extends RegistrationCommonFields implements Serializable {

	private static final long serialVersionUID = -8541947587557590379L;

	@Id
	@Column(name = "code")
	private String code;

	@Column(name = "name")
	private String name;

	@Column(name = "descr")
	private String description;


	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

}
