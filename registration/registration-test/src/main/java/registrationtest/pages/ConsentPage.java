package registrationtest.pages;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testfx.api.FxRobot;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.WaitsUtil;

public class ConsentPage {
	private static final Logger logger = LogManager.getLogger(ConsentPage.class); 
	WaitsUtil waitsUtil;
	String consentTextara="#consentTextara";
	String consentTexteng="#consentTexteng";
	FxRobot robot;
	public ConsentPage(FxRobot robot) {
		this.robot=robot;
	}

	public void consentCheck() {

		try {	
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				javafx.scene.web.WebView vieweng=waitsUtil.lookupById(consentTexteng);

				System.out.println(vieweng.getEngine().executeScript("document.body.innerHTML;"));
				
				javafx.scene.web.WebView viewara=waitsUtil.lookupById(consentTextara);

				System.out.println(viewara.getEngine().executeScript("document.body.innerHTML;"));
				
			//	String RegistrationID=(String) mywebview.getEngine().executeScript("document.body.getElementsByTagName('td')[0].innerHTML;");
				
				//mywebview.getEngine().executeScript("document.getElementById('consent-yes').click();");
			}
		});
Thread.sleep(90000);
		//Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("WebviewTimeWait"))); 
		}catch(Exception e)
		{
			logger.error(e.getMessage());
		}
	
	}

}
