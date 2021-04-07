package registrationtest.pages;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testfx.api.FxRobot;

import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.WaitsUtil;

public class BiometricUploadPage {

	private static final Logger logger = LogManager.getLogger(BiometricUploadPage.class); 
	FxRobot robot;
	WaitsUtil waitsUtil;
	String BiometricDetail="#BiometricDetail";
	String scanBtn="#scanBtn";
	String IRIS_DOUBLE="#IRIS_DOUBLE";
	String FINGERPRINT_SLAB_RIGHT="#FINGERPRINT_SLAB_RIGHT";
	String FINGERPRINT_SLAB_LEFT="#FINGERPRINT_SLAB_LEFT";
	String FINGERPRINT_SLAB_THUMBS="#FINGERPRINT_SLAB_THUMBS";
	String FACE="#FACE";
	String alertImage="#alertImage";
	String exit="#exit";
	String success="Success";


	BiometricUploadPage(FxRobot robot)
	{logger.info("BiometricUploadPage Constructor");

	this.robot=robot;
	waitsUtil=new WaitsUtil(robot);
	//waitsUtil.clickNodeAssert( BiometricDetail);

	}


	public void bioSpecificScan(String id,String scanBtn)
	{
		try {
			logger.info("bioSpecificScan");
			waitsUtil.clickNodeAssert( id);
			waitsUtil.clickNodeAssert( scanBtn);
			try {
				Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("wait15")));
				waitsUtil.clickNodeAssert( alertImage);

				waitsUtil.clickNodeAssert( success);
				waitsUtil.clickNodeAssert( exit);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			
		
			//robot.press(KeyCode.SPACE).release(KeyCode.SPACE);
		}
		catch(InterruptedException e)
		{
			logger.error(e.getMessage());
		}


	}
	/**
	 * Bio attributes without List
	 * @return
	 * @throws InterruptedException
	 */
	public WebViewDocument biometricDetailsUpload() {

		try
		{logger.info("  Bio attributes upload without List");
		bioSpecificScan(IRIS_DOUBLE,scanBtn);

		bioSpecificScan(FINGERPRINT_SLAB_RIGHT,scanBtn);

		bioSpecificScan(FINGERPRINT_SLAB_LEFT,scanBtn);


		bioSpecificScan(FINGERPRINT_SLAB_THUMBS,scanBtn);


		bioSpecificScan(FACE,scanBtn);
		}
		catch(Exception e)
		{
			logger.error(e.getMessage());
		}

		return new WebViewDocument(robot);
	}

	/**
	 * Bio attributes in list
	 * @param list
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public WebViewDocument newRegbioUpload(List<String> list)  {
		try
		{
			logger.info("  Bio attributes upload with List");
			if(list.contains(PropertiesUtil.getKeyValue("leftEye"))||list.contains(PropertiesUtil.getKeyValue("rightEye")))
				bioSpecificScan(IRIS_DOUBLE,scanBtn);

			if(list.contains(PropertiesUtil.getKeyValue("rightIndex"))||list.contains(PropertiesUtil.getKeyValue("rightLittle"))||list.contains(PropertiesUtil.getKeyValue("rightRing"))||list.contains(PropertiesUtil.getKeyValue("rightMiddle")))

				bioSpecificScan(FINGERPRINT_SLAB_RIGHT,scanBtn);

			if(list.contains(PropertiesUtil.getKeyValue("leftIndex"))||list.contains(PropertiesUtil.getKeyValue("leftLittle"))||list.contains(PropertiesUtil.getKeyValue("leftRing"))||list.contains(PropertiesUtil.getKeyValue("leftMiddle")))

				bioSpecificScan(FINGERPRINT_SLAB_LEFT,scanBtn);

			if(list.contains(PropertiesUtil.getKeyValue("leftThumb"))||list.contains(PropertiesUtil.getKeyValue("rightThumb")))
				bioSpecificScan(FINGERPRINT_SLAB_THUMBS,scanBtn);

			if(list.contains(PropertiesUtil.getKeyValue("face")))
				bioSpecificScan(FACE,scanBtn);
		}
		catch(Exception e)
		{
			logger.error(e.getMessage());
		}

		return new WebViewDocument(robot);

	}


	public void newRegbioUpload(String biostring) {
		// TODO Auto-generated method stub
		try {
			if(biostring.contains(PropertiesUtil.getKeyValue("face")))
				bioSpecificScan(FACE,scanBtn);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
		
	}

}
