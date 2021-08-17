package io.mosip.registration.dto;

import java.math.BigInteger;

import lombok.Data;

/**
 * The DTO Class PacketStatusDTO.
 * 
 * @author Sreekar Chukka
 * @since 1.0.0
 */
@Data
public class PacketStatusDTO {

	private String fileName;
	private String packetId;
	private String packetClientStatus;
	private String packetServerStatus;
	private String packetPath;
	private String uploadStatus;
	private String clientStatusComments;
	private String packetStatus;
	private String supervisorStatus;
	private String supervisorComments;
	private BigInteger packetSize;
	private String packetHash;
	private String createdTime;
	private String name;
	private String phone;
	private String email;
	private String userId;
	private String selectedLanguages;
}