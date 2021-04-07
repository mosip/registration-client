package registrationtest.pojo.testdata;

public class TestData {

	public String id;
	    public Boolean inputRequired;
	    public String value
	   ;
	    TestData(){}
		public TestData(String id, Boolean inputRequired, String value) {
			super();
			this.id = id;
			this.inputRequired = inputRequired;
			this.value = value;
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public Boolean getInputRequired() {
			return inputRequired;
		}
		public void setInputRequired(Boolean inputRequired) {
			this.inputRequired = inputRequired;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	   
	  
}
