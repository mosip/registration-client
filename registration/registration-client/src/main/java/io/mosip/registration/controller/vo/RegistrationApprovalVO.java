package io.mosip.registration.controller.vo;

import javafx.beans.property.SimpleStringProperty;

/**
 * The Class RegistrationApprovalVO.
 * 
 * @author Mahesh Kumar
 */
public class RegistrationApprovalVO {

	private SimpleStringProperty slno;
	private SimpleStringProperty id;
	private SimpleStringProperty packetId;
	private SimpleStringProperty date;
	private SimpleStringProperty acknowledgementFormPath;
	private SimpleStringProperty operatorId;
	private SimpleStringProperty statusComment;

	/**
	 * Instantiates a new registration approval VO.
	 *
	 * @param id                      the id
	 * @param acknowledgementFormPath the acknowledgement form path
	 * @param statusComment           the status comment
	 */
	public RegistrationApprovalVO(String slno, String id, String packetId, String date, String acknowledgementFormPath, String operatorId, String statusComment) {
		super();
		this.slno = new SimpleStringProperty(slno);
		this.id = new SimpleStringProperty(id);
		this.packetId = new SimpleStringProperty(packetId);
		this.date = new SimpleStringProperty(date);
		this.acknowledgementFormPath = new SimpleStringProperty(acknowledgementFormPath);
		this.operatorId = new SimpleStringProperty(operatorId);
		this.statusComment = new SimpleStringProperty(statusComment);
	}

	/**
	 * @return the slno
	 */
	public String getSlno() {
		return slno.get();
	}
	
	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public String getId() {
		return id.get();
	}
	
	/**
	 * Gets the packetId.
	 *
	 * @return the packetId
	 */
	public String getPacketId() {
		return packetId.get();
	}

	/**
	 * @return the date
	 */
	public String getDate() {
		return date.get();
	}
	
	/**
	 * Gets the acknowledgement form path.
	 *
	 * @return the acknowledgementFormPath
	 */
	public String getAcknowledgementFormPath() {
		return acknowledgementFormPath.get();
	}

	/**
	 * Gets the status comment.
	 *
	 * @return the statusComment
	 */
	public String getStatusComment() {
		return statusComment.get();
	}
	
	public String getOperatorId() {
		return operatorId.get();
	}
}
