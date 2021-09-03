package io.mosip.registration.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import io.mosip.registration.entity.Template;

/**
 * This class is used to fetch the list of templates from {@link Template} table by passing 
 * template type code as parameter.
 * 
 * @author Himaja Dhanyamraju
 *
 */
@Repository
public interface TemplateDao {

	/**
	 * This method returns the list of templates which are active and have specified
	 * templateTypeCode
	 * 
	 * @param templateTypeCode
	 *            the required template type code
	 * @return the list of {@link Template}
	 */
	List<Template> getAllTemplates(String templateTypeCode);

	List<Template> getAllTemplates(String templateTypeCode, String langCode);
}
