package io.mosip.registration.repositories;

import java.util.List;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.registration.entity.LocalPreferences;

public interface LocalPreferencesRepository extends BaseRepository<LocalPreferences, String> {
	
	LocalPreferences findByIsDeletedFalseAndName(String name);
	
	List<LocalPreferences> findByIsDeletedFalseAndConfigType(String configType);

}
