package registrationtest.pojo.schema;

// import com.fasterxml.jackson.databind.ObjectMapper; // version 2.11.1
// import com.fasterxml.jackson.annotation.JsonProperty; // version 2.11.1
/* ObjectMapper om = new ObjectMapper();
Root root = om.readValue(myJsonString), Root.class); */
public class Label {
    public String getEng() {
		return eng;
	}
	public void setEng(String eng) {
		this.eng = eng;
	}
	public String getAra() {
		return ara;
	}
	public void setAra(String ara) {
		this.ara = ara;
	}
	public String getFra() {
		return fra;
	}
	public void setFra(String fra) {
		this.fra = fra;
	}
	public String eng;
    public String ara;
    public String fra;

}
