package qa114.testcases;

import java.io.IOException;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import qa114.runapplication.NewRegistrationAdultTest;
import qa114.utility.PropertiesUtil;

public class JunitClass {


	@Test
	public void testcase1()
	{
		System.setProperty("java.net.useSystemProxies", "true");
		System.setProperty("file.encoding", "UTF-8");
		System.setProperty("derby.ui.codeset", "UTF-8");

		try {
			System.setProperty("mosip.upgradeserver",PropertiesUtil.getKeyValue("mosip.upgradeserver"));

			System.setProperty("mosip.hostname",PropertiesUtil.getKeyValue("mosip.hostname"));

			System.setProperty("jdbc.drivers","org.apache.derby.jdbc.EmbeddedDriver");

			NewRegistrationAdultTest.invokeRegClient(PropertiesUtil.getKeyValue("operatorId"), 
					PropertiesUtil.getKeyValue("operatorPwd"),PropertiesUtil.getKeyValue("mosip.upgradeserver"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



	}


	@Test
	public void testcase2()
	{
		System.setProperty("java.net.useSystemProxies", "true");
		System.setProperty("file.encoding", "UTF-8");
		System.setProperty("derby.ui.codeset", "UTF-8");

	
		try {
			NewRegistrationAdultTest.invokeRegClientNewReg(NewRegistrationAdultTest.readJsonFileText(),
					NewRegistrationAdultTest.readMapDocumentValues(),
					PropertiesUtil.getKeyValue("operatorId"), 
					PropertiesUtil.getKeyValue("operatorPwd"),
					PropertiesUtil.getKeyValue("supervisorUserid"), 
					PropertiesUtil.getKeyValue("supervisorUserpwd"),
					Double.parseDouble(PropertiesUtil.getKeyValue("schemaversion")),
					PropertiesUtil.getKeyValue("mosip.upgradeserver"));
		} catch (NumberFormatException | JSONException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
