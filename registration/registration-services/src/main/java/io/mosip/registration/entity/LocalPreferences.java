package io.mosip.registration.entity;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema = "reg", name = "local_preferences")
@Getter
@Setter
public class LocalPreferences {
	
	@Id
	@Column(name = "id")
	private String id;
	@Column(name = "name")
	private String name;
	@Column(name = "val")
	private String val;
	@Column(name = "config_type")
	private String configType;
	@Column(name = "machine_name")
	private String machineName;
	@Column(name = "CR_BY")
	protected String crBy;
	@Column(name = "CR_DTIMES")
	protected Timestamp crDtime;
	@Column(name = "UPD_BY")
	protected String updBy;
	@Column(name = "UPD_DTIMES")
	protected Timestamp updDtimes;
	@Column(name = "is_deleted")
	private Boolean isDeleted;
	@Column(name = "del_dtimes")
	private Timestamp delDtimes;
}
