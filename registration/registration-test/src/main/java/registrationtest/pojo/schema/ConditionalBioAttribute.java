package registrationtest.pojo.schema;

import java.util.List;

public class ConditionalBioAttribute {

	public String ageGroup;
    public String process;
    public String validationExpr;
    public List<String> bioAttributes;
	public String getAgeGroup() {
		return ageGroup;
	}
	public void setAgeGroup(String ageGroup) {
		this.ageGroup = ageGroup;
	}
	public String getProcess() {
		return process;
	}
	public void setProcess(String process) {
		this.process = process;
	}
	public String getValidationExpr() {
		return validationExpr;
	}
	public void setValidationExpr(String validationExpr) {
		this.validationExpr = validationExpr;
	}
	public List<String> getBioAttributes() {
		return bioAttributes;
	}
	public void setBioAttributes(List<String> bioAttributes) {
		this.bioAttributes = bioAttributes;
	}
    
}
