package io.mosip.registration.dao;

import java.time.LocalDateTime;
import java.util.List;

import io.mosip.kernel.auditmanager.entity.Audit;

/**
 * This class is used to fetch/delete audit related information to {@link Audit} table.
 * DAO class for Audit
 * 
 * @author Balaji Sridharan
 * @author Yaswanth S
 * @since 1.0.0
 */
public interface AuditDAO {

	/**
	 * This method is used to delete all audit rows which are present before the given specific time.
	 *            
	 * @param auditLogToDtimes
	 *            end time
	 */
	void deleteAudits(LocalDateTime auditLogToDtimes);
	
	/**
	 * This method is used to retrieve the {@link Audit} logs which are yet to be synchronized to the
	 * server along with the registration packet
	 *            
	 * @return the {@link Audit} logs to be synchronized to the server with
	 *         registration packet
	 */
	List<Audit> getAudits(String registrationId, String timestamp);

}
