package io.mosip.registration.dto;

/**
 * The Class RegistrationApprovalDTO.
 * 
 * @author Mahesh Kumar
 */
public class RegistrationApprovalDTO {

	private String id;
	private String packetId;
	private String date;
	private String acknowledgementFormPath;
	private String operatorId;
	private String statusComment;
	private boolean hasBwords;

	/**
	 * Instantiates a new registration approval DTO.
	 *
	 * @param id 
	 * 				the id
	 * @param date
	 * 				the date of registration
	 * @param acknowledgementFormPath 
	 * 				the acknowledgement form path
	 * @param statusComment 
	 * 				the status comment
	 */
	public RegistrationApprovalDTO(String id, String packetId, String date, String acknowledgementFormPath, String operatorId, String statusComment, boolean hasBwords) {
		super();
		
		this.id = id;
		this.packetId = packetId;
		this.date = date;
		this.acknowledgementFormPath =acknowledgementFormPath;
		this.operatorId = operatorId;
		this.statusComment = statusComment;
		this.hasBwords = hasBwords;
	}
	
	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Gets the packetId.
	 *
	 * @return the packetId
	 */
	public String getPacketId() {
		return packetId;
	}

	/**
	 * @return the date
	 */
	public String getDate() {
		return date;
	}
	
	/**
	 * Gets the acknowledgement form path.
	 *
	 * @return the acknowledgementFormPath
	 */
	public String getAcknowledgementFormPath() {
		return acknowledgementFormPath;
	}

	/**
	 * Gets the status comment.
	 *
	 * @return the statusComment
	 */
	public String getStatusComment() {
		return statusComment;
	}
	
	public String getOperatorId() {
		return operatorId;
	}
	
	public boolean getHasBwords() {
		return hasBwords;
	}
}
