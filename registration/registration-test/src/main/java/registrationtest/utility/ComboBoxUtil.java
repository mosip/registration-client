package registrationtest.utility;

import io.mosip.registration.dto.mastersync.GenericDto;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javafx.application.Platform;
import javafx.scene.control.ComboBox;

public class ComboBoxUtil {
	private static final Logger logger = LogManager.getLogger(ComboBoxUtil.class); 
	static WaitsUtil waitsUtil=new WaitsUtil();
		
	public static void user_selects_combo_item(String comboBoxId, GenericDto dto)  {
		Thread taskThread = new Thread(new Runnable() {
			@Override
			public void run() {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						ComboBox comboBox= waitsUtil.lookupById(comboBoxId);

						comboBox.getSelectionModel().select(dto); 
					}}); 
			}});
		taskThread.start();
		try {
			taskThread.join();
			Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("ComboItemTimeWait")));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		} 

	}


}





