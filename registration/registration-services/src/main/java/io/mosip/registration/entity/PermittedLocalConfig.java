package io.mosip.registration.entity;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema = "reg", name = "permitted_local_config")
@Getter
@Setter
public class PermittedLocalConfig extends RegistrationCommonFields {
	
	@Id
	@Column(name = "code")
	private String code;
	
	@Column(name = "name")
	private String name;
	@Column(name = "config_type")
	private String type;
	@Column(name = "is_deleted")
	private Boolean isDeleted;
	@Column(name = "del_dtimes")
	private Timestamp delDtimes;
	
	/**
	 * @param updDtimes the updDtimes to set
	 */
	@Override
	public void setUpdDtimes(Timestamp updDtimes) {
		this.updDtimes = updDtimes;
	}

}
