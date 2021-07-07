package io.mosip.registration.entity;

import java.sql.Date;

import javax.persistence.*;

import lombok.Getter;
import lombok.Setter;

/**
 * The Entity Class for Template FileFormat details
 * 
 * @author Himaja Dhanyamraju
 * @since 1.0.0
 */
@Entity
@Table(schema="reg", name = "TEMPLATE_FILE_FORMAT")
@Getter
@Setter
public class TemplateFileFormat extends RegistrationCommonFields {

	@Id
	@Column(name="code")
	private String code;
	@Column(name="lang_code")
	private String langCode;
	@Column(name="descr")
	private String description;
	@Column(name="is_deleted")
	private Boolean isDeleted;
	@Column(name="del_dtimes")
	private Date delDtimes;

}
