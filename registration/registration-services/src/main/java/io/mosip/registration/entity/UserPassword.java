package io.mosip.registration.entity;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * The Entity Class for User Password details
 * 
 * @author Sravya Surampalli
 * @since 1.0.0
 */
@Entity
@Table(schema = "reg", name = "user_pwd")
@Getter
@Setter
public class UserPassword extends RegistrationCommonFields {

	@OneToOne
	@JoinColumn(name = "usr_id", nullable = false, insertable = false, updatable = false)
	private UserDetail userDetail;

	@Id
	@Column(name = "usr_id")
	private String usrId;
	@Column(name = "pwd")
	private String pwd;
	@Column(name = "pwd_expiry_dtimes")
	private Timestamp pwdExpiryDtimes;
	@Column(name = "status_code")
	private String statusCode;
	@Column(name = "lang_code")
	private String langCode;
	@Column(name = "is_deleted")
	private Boolean isDeleted;
	@Column(name = "del_dtimes")
	private Timestamp delDtimes;

}
