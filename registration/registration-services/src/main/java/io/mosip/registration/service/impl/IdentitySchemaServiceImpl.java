package io.mosip.registration.service.impl;

import java.util.ArrayList;
import java.util.List;

import io.mosip.registration.dto.schema.ProcessSpecDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.dao.IdentitySchemaDao;
import io.mosip.registration.dto.schema.SettingsSchema;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.dto.schema.SchemaDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.IdentitySchemaService;

@Service
public class IdentitySchemaServiceImpl implements IdentitySchemaService {
	
	private static final Logger LOGGER = AppConfig.getLogger(IdentitySchemaServiceImpl.class);

	@Autowired
	private IdentitySchemaDao identitySchemaDao;
	
	@Override
	public Double getLatestEffectiveSchemaVersion() throws RegBaseCheckedException {
		return identitySchemaDao.getLatestEffectiveSchemaVersion();
	}

	/*@Override
	public List<UiSchemaDTO> getLatestEffectiveUISchema() throws RegBaseCheckedException {
		return identitySchemaDao.getLatestEffectiveUISchema();
	}*/

	@Override
	public String getLatestEffectiveIDSchema() throws RegBaseCheckedException {
		return identitySchemaDao.getLatestEffectiveIDSchema();
	}

	/*@Override
	public List<UiSchemaDTO> getUISchema(double idVersion) throws RegBaseCheckedException {
		return identitySchemaDao.getUISchema(idVersion);
	}*/

	@Override
	public String getIDSchema(double idVersion) throws RegBaseCheckedException {
		return identitySchemaDao.getIDSchema(idVersion);
	}

	@Override
	public SchemaDto getIdentitySchema(double idVersion) throws RegBaseCheckedException {
		return identitySchemaDao.getIdentitySchema(idVersion);
	}
	
	@Override
	public List<SettingsSchema> getSettingsSchema(double idVersion) throws RegBaseCheckedException {
		return identitySchemaDao.getSettingsSchema(idVersion);
	}

	@Override
	public ProcessSpecDto getProcessSpecDto(String processId, double idVersion) throws RegBaseCheckedException {
		return identitySchemaDao.getProcessSpec(processId, idVersion);
	}

	@Override
	public List<UiFieldDTO> getAllFieldSpec(String processId, double idVersion) throws RegBaseCheckedException {
		List<UiFieldDTO> schemaFields = new ArrayList<>();
		ProcessSpecDto processSpecDto = getProcessSpecDto(processId, idVersion);
		processSpecDto.getScreens().forEach(screen -> {
			schemaFields.addAll(screen.getFields());
		});
		return schemaFields;
	}

}
