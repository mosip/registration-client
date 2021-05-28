
package registrationtest.pojo.schema;

import java.util.List;

public class Screens{
    public int order;
    public String name;
    public Label label;
    public Caption caption;
    public List<String> fields;
    public Object layoutTemplate;
    public boolean preRegFetchRequired;
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
	public Label getLabel() {
		return label;
	}
	public void setLabel(Label label) {
		this.label = label;
	}
	public Caption getCaption() {
		return caption;
	}
	public void setCaption(Caption caption) {
		this.caption = caption;
	}
	public List<String> getFields() {
		return fields;
	}
	public void setFields(List<String> fields) {
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
