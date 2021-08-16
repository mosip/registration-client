package registrationtest.utility;

import io.mosip.registration.dto.mastersync.GenericDto;

import java.util.Optional;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javafx.application.Platform;
import javafx.scene.control.ComboBox;

public class ComboBoxUtil {
	private static final Logger logger = LogManager.getLogger(ComboBoxUtil.class); 
	static WaitsUtil waitsUtil=new WaitsUtil();
		
	public static void user_selects_combo_item(String comboBoxId, String val)  {
		Thread taskThread = new Thread(new Runnable() {
			@Override
			public void run() {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						ComboBox comboBox= waitsUtil.lookupById(comboBoxId);

						Optional<GenericDto> op=comboBox.getItems().stream().filter(i->((GenericDto)i).getName().equalsIgnoreCase(val)).findFirst();
						if(op.isEmpty())
							comboBox.getSelectionModel().selectFirst();
						else 
							comboBox.getSelectionModel().select(op.get());
					}}); 
			}});
		taskThread.start();
		try {
			taskThread.join();
			Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("ComboItemTimeWait")));
		} catch (Exception e) {
			
			logger.error("",e);
		} 

	}


}





