	
package registrationtest.pojo.schema;

import java.util.HashMap;
import java.util.List;

public class Screens{
    public int order;
    public String name;
    public HashMap<String, String> label;
    public HashMap<String, String> caption;
    public List<Schema> fields;
    public Object layoutTemplate;
    public boolean preRegFetchRequired;
    public boolean additionalInfoRequestIdRequired;
    
    public boolean isAdditionalInfoRequestIdRequired() {
		return additionalInfoRequestIdRequired;
	}
	public void setAdditionalInfoRequestIdRequired(boolean additionalInfoRequestIdRequired) {
		this.additionalInfoRequestIdRequired = additionalInfoRequestIdRequired;
	}
	public boolean active;
	public int getOrder() {
		return order;
	}
	public void setOrder(int order) {
		this.order = order;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public HashMap<String, String> getLabel() {
		return label;
	}
	public void setLabel(HashMap<String, String> label) {
		this.label = label;
	}
	public HashMap<String, String> getCaption() {
		return caption;
	}
	public void setCaption(HashMap<String, String> caption) {
		this.caption = caption;
	}
	public List<Schema> getFields() {
		return fields;
	}
	public void setFields(List<Schema> fields) {
		this.fields = fields;
	}
	public Object getLayoutTemplate() {
		return layoutTemplate;
	}
	public void setLayoutTemplate(Object layoutTemplate) {
		this.layoutTemplate = layoutTemplate;
	}
	public boolean isPreRegFetchRequired() {
		return preRegFetchRequired;
	}
	public void setPreRegFetchRequired(boolean preRegFetchRequired) {
		this.preRegFetchRequired = preRegFetchRequired;
	}
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
    
}
