package registrationtest.pojo.testdata;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RootTestData {

    @JsonProperty("TestData") 
    public List<TestData> testData;
    
    RootTestData(){}

	public RootTestData(List<TestData> testData) {
		super();
		this.testData = testData;
	}

	public List<TestData> getTestData() {
		return testData;
	}

	public void setTestData(List<TestData> testData) {
		this.testData = testData;
	}
    
    
}
