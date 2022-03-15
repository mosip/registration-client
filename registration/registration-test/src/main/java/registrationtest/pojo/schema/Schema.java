package registrationtest.pojo.schema;

import io.mosip.registration.constants.RegistrationConstants;

import java.util.HashMap;
import java.util.List;

public class Schema {
    public String id;
    public boolean inputRequired;
    public String type;
    public int minimum;
    public int maximum;
    public String description;
    HashMap<String, String> label;
    public String controlType;
    public String fieldType;
    public String format;
    public List<Validator> validators;
    public String fieldCategory;
    public Object alignmentGroup;
    public Object visible;
    public String contactType;
    public String group;
    public Object groupLabel;
    public Object changeAction;
    public boolean transliterate;
    public boolean isTransliterate() {
		return transliterate;
	}
	public void setTransliterate(boolean transliterate) {
		this.transliterate = transliterate;
	}
	public String templateName;
    public Object fieldLayout;
    public Object locationHierarchy;
    public List<ConditionalBioAttribute> conditionalBioAttributes;
    
   
	public boolean required;
    public List<String> bioAttributes;
    public List<RequiredOn> requiredOn;
    public String subType;
    public boolean exceptionPhotoRequired;
    
  	public Schema()
    {}
	public Schema(String id, boolean inputRequired, String type, int minimum, int maximum, String description,
			HashMap<String, String> label, String controlType, String fieldType, String format, List<Validator> validators,
			String fieldCategory, Object alignmentGroup, Object visible, String contactType, String group,
			Object changeAction, boolean required, List<String> bioAttributes, List<RequiredOn> requiredOn,
			String subType,Boolean exceptionPhotoRequired) {
		super();
		this.id = id;
		this.inputRequired = inputRequired;
		this.type = type;
		this.minimum = minimum;
		this.maximum = maximum;
		this.description = description;
		this.label = label;
		this.controlType = controlType;
		this.fieldType = fieldType;
		this.format = format;
		this.validators = validators;
		this.fieldCategory = fieldCategory;
		this.alignmentGroup = alignmentGroup;
		this.visible = visible;
		this.contactType = contactType;
		this.group = group;
		this.changeAction = changeAction;
		this.required = required;
		this.bioAttributes = bioAttributes;
		this.requiredOn = requiredOn;
		this.subType = subType;
		this.exceptionPhotoRequired=exceptionPhotoRequired;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public boolean isInputRequired() {
		return inputRequired;
	}
	public void setInputRequired(boolean inputRequired) {
		this.inputRequired = inputRequired;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public int getMinimum() {
		return minimum;
	}
	public void setMinimum(int minimum) {
		this.minimum = minimum;
	}
	public int getMaximum() {
		return maximum;
	}
	public void setMaximum(int maximum) {
		this.maximum = maximum;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getControlType() {
		return controlType;
	}
	public void setControlType(String controlType) {
		this.controlType = controlType;
	}
	public String getFieldType() {
		return fieldType;
	}
	public void setFieldType(String fieldType) {
		this.fieldType = fieldType;
	}
	public String getFormat() {
		return format;
	}
	public void setFormat(String format) {
		this.format = format;
	}
	public List<Validator> getValidators() {
		return validators;
	}
	public void setValidators(List<Validator> validators) {
		this.validators = validators;
	}
	public String getFieldCategory() {
		return fieldCategory;
	}
	public void setFieldCategory(String fieldCategory) {
		this.fieldCategory = fieldCategory;
	}
	public Object getAlignmentGroup() {
		return alignmentGroup;
	}
	public void setAlignmentGroup(Object alignmentGroup) {
		this.alignmentGroup = alignmentGroup;
	}
	public Object getVisible() {
		return visible;
	}
	public void setVisible(Object visible) {
		this.visible = visible;
	}
	public String getContactType() {
		return contactType;
	}
	public void setContactType(String contactType) {
		this.contactType = contactType;
	}
	public String getGroup() {
		return group;
	}
	public void setGroup(String group) {
		this.group = group;
	}
	public Object getChangeAction() {
		return changeAction;
	}
	public void setChangeAction(Object changeAction) {
		this.changeAction = changeAction;
	}
	public boolean isRequired() {
		return required;
	}
	public void setRequired(boolean required) {
		this.required = required;
	}
	public List<String> getBioAttributes() {
		return bioAttributes;
	}
	public void setBioAttributes(List<String> bioAttributes) {
		this.bioAttributes = bioAttributes;
	}
	public List<RequiredOn> getRequiredOn() {
		return requiredOn;
	}
	public void setRequiredOn(List<RequiredOn> requiredOn) {
		this.requiredOn = requiredOn;
	}
	public String getSubType() {
		return subType;
	}
	public void setSubType(String subType) {
		this.subType = subType;
	}
	 public HashMap<String, String> getLabel() {
			return label;
		}
		public void setLabel(HashMap<String, String> label) {
			this.label = label;
		}
		
    
		 public List<ConditionalBioAttribute> getConditionalBioAttributes() {
				return conditionalBioAttributes;
			}
			public void setConditionalBioAttributes(List<ConditionalBioAttribute> conditionalBioAttributes) {
				this.conditionalBioAttributes = conditionalBioAttributes;
			}
			  public boolean isExceptionPhotoRequired() {
				  return exceptionPhotoRequired ||
						  (subType != null && RegistrationConstants.APPLICANT.equalsIgnoreCase(subType));
				}
				public void setExceptionPhotoRequired(boolean exceptionPhotoRequired) {
					this.exceptionPhotoRequired = exceptionPhotoRequired;
				}

}
