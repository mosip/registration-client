package io.mosip.registration.repositories;

import java.util.List;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.registration.entity.PermittedLocalConfig;

public interface PermittedLocalConfigRepository extends BaseRepository<PermittedLocalConfig, String> {
	
	/**
	 * Retrieving permitted local configurations.
	 *
	 * @return list of permitted local configs
	 */
	List<PermittedLocalConfig> findByIsActiveTrue();
	
	List<PermittedLocalConfig> findByIsActiveTrueAndType(String configType);

}
