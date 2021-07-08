package registrationtest.pages;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testfx.api.FxRobot;

import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import registrationtest.controls.Buttons;
import registrationtest.utility.JsonUtil;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.WaitsUtil;

public class BiometricUploadPage {

	private static final Logger logger = LogManager.getLogger(BiometricUploadPage.class); 
	FxRobot robot;
	WaitsUtil waitsUtil;
	String BiometricDetail="#BiometricDetail";
	String scanBtn="ScanBtn";
	String IRIS_DOUBLE="IRIS_DOUBLE";
	String FINGERPRINT_SLAB_RIGHT="FINGERPRINT_SLAB_RIGHT";
	String FINGERPRINT_SLAB_LEFT="FINGERPRINT_SLAB_LEFT";
	String FINGERPRINT_SLAB_THUMBS="FINGERPRINT_SLAB_THUMBS";
	String FACE="FACE";
	String EXCEPTION_PHOTO="EXCEPTION_PHOTO";
	String alertImage="#alertImage";
	String exit="#exit";
	String success="#context";
	Buttons buttons;
	
	
	BiometricUploadPage(FxRobot robot)
	{logger.info("BiometricUploadPage Constructor");

	this.robot=robot;
	waitsUtil=new WaitsUtil(robot);
	buttons=new Buttons(robot);
	//waitsUtil.clickNodeAssert( BiometricDetail);

	}

	public void exceptionsIrisDouble(String id,String identity,String subType)
	{

		logger.info("  Bio attributes upload with List");

		List<String> listException=null;
		Boolean flag=false;
		try {
			listException = exceptionList(identity);
			// IRIS DOUBLE
			waitsUtil.clickNodeAssert(id);
			Thread.sleep(400);
			
			if(listException.contains("leftEye")&&listException.contains("rightEye"))
			{
				waitsUtil.clickNodeAssert("#leftEye");
				waitsUtil.clickNodeAssert("#rightEye");
				flag=true;
			}
			else if (listException.contains("leftEye"))
			{
				waitsUtil.clickNodeAssert("#leftEye");
			}
			else if (listException.contains("rightEye"))
			{
				waitsUtil.clickNodeAssert("#rightEye");
			}
			if(flag==false)
			clickScanBtn(subType);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	public void exceptionsFingerPrintSlabThumbs(String id,String identity,String subType)
	{

		logger.info("  Bio attributes upload with List");

		List<String> listException=null;
		Boolean flag=false;
		try {listException = exceptionList(identity);
			// FINGERPRINT_SLAB_THUMBS
			waitsUtil.clickNodeAssert(id);
			Thread.sleep(400);
			
			if(listException.contains("leftThumb")&&listException.contains("rightThumb"))
			{
				waitsUtil.clickNodeAssert("#leftThumb");
				waitsUtil.clickNodeAssert("#rightThumb");
				flag=true;
			}
			else if (listException.contains("leftThumb"))
			{
				waitsUtil.clickNodeAssert("#leftThumb");
			}
			else if (listException.contains("rightThumb"))
			{
				waitsUtil.clickNodeAssert("#rightThumb");
			}
			if(flag==false)
			clickScanBtn(subType);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}
	
	public void exceptionsFingerPrintSlabRight(String id,String identity,String subType)
	{

		logger.info("  Bio attributes upload with List");

		List<String> listException=null;
		Boolean flag=false;
		try {
			listException = exceptionList(identity);
			//FINGERPRINT_SLAB_RIGHT
			waitsUtil.clickNodeAssert(id);
			Thread.sleep(400);
			
			if(listException.contains("rightIndex")&&listException.contains("rightLittle")&&listException.contains("rightRing")&&listException.contains("rightMiddle"))
			{
				waitsUtil.clickNodeAssert("#rightIndex");
				waitsUtil.clickNodeAssert("#rightLittle");
				waitsUtil.clickNodeAssert("#rightRing");
				waitsUtil.clickNodeAssert("#rightMiddle");
				flag=true;
			}else {
			 if (listException.contains("rightIndex"))
			{
				waitsUtil.clickNodeAssert("#rightIndex");
			}
			 if (listException.contains("rightLittle"))
			{
				waitsUtil.clickNodeAssert("#rightLittle");
			}
			 if (listException.contains("rightRing"))
				{
					waitsUtil.clickNodeAssert("#rightRing");
				}
				 if (listException.contains("rightMiddle"))
				{
					waitsUtil.clickNodeAssert("#rightMiddle");
				}}
			if(flag==false)
			clickScanBtn(subType);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	
	public List<String> bioAuthAttributeList(String identity)
	{
		List<String> bioAuthAttList=new LinkedList<String>();
		String bioAuthAttributes=null;
		try {
			 bioAuthAttributes=PropertiesUtil.getKeyValue("bioAuthAttributes");
		
		
		bioAuthAttList=JsonUtil.JsonObjArrayListParsing(identity, bioAuthAttributes);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		
		return bioAuthAttList;
	}

	public List<String> exceptionList(String identity)
{
	List<String> listException=new LinkedList<String>();
	String bioExceptionAttributes=null;
	try {
		 bioExceptionAttributes=PropertiesUtil.getKeyValue("bioExceptionAttributes");
	
	
	listException=JsonUtil.JsonObjArrayListParsing(identity, bioExceptionAttributes);
	} catch (Exception e) {
		logger.error(e.getMessage());
	}
	
	return listException;
}
	
	public void exceptionsFingerPrintSlabLeft(String id,String identity,String subType)
	{

		logger.info("exceptionsFingerPrintSlabLeft");

		List<String> listException=null;
		Boolean flag=false;
		try {
			listException = exceptionList(identity);
			//FINGERPRINT_SLAB_LEFT
			waitsUtil.clickNodeAssert(id);
			Thread.sleep(400);
			
			if(listException.contains("leftIndex")&&listException.contains("leftLittle")&&
					listException.contains("leftRing")&&listException.contains("leftMiddle"))
			{
				waitsUtil.clickNodeAssert("#leftIndex");
				waitsUtil.clickNodeAssert("#leftLittle");
				waitsUtil.clickNodeAssert("#leftRing");
				waitsUtil.clickNodeAssert("#leftMiddle");
				flag=true;
			}else {
			 if (listException.contains("leftIndex"))
			{
				waitsUtil.clickNodeAssert("#leftIndex");
			}
			 if (listException.contains("leftLittle"))
			{
				waitsUtil.clickNodeAssert("#leftLittle");
			}
			 if (listException.contains("leftRing"))
				{
					waitsUtil.clickNodeAssert("#leftRing");
				}
				 if (listException.contains("leftMiddle"))
				{
					waitsUtil.clickNodeAssert("#leftMiddle");
				}}
			if(flag==false)
			clickScanBtn(subType);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	
	public void clickScanBtn(String subType)
	{waitsUtil.clickNodeAssert( "#"+subType+scanBtn);

	try {
		Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("wait6")));
	}  catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	waitsUtil.clickNodeAssert( alertImage);

	waitsUtil.clickNodeAssert( success);
	waitsUtil.clickNodeAssert( exit);
	}
	
	
	public void bioScan(String subtype,String id,String identity)
	{
		try {
			logger.info("bioScan");
			waitsUtil.clickNodeAssert(id);
			//waitsUtil.scrollclickNodeAssert(id);
			Thread.sleep(400);
			clickScanBtn(subtype);
		}
		catch(Exception e)
		{
			logger.error(e.getMessage());
		}


	}

	/**
	 * Bio attributes in list
	 * @param list
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void newRegbioUpload(String subtype,List<String> list,String id,String identity)  {
		try

		{
			logger.info("Bio attributes upload with List");

			List<String> listException = null;
			try {
				listException = exceptionList(identity);
			}catch(Exception e)
			{
				logger.error(e.getMessage());
			}
			if(listException.isEmpty()){
				if(list.contains(PropertiesUtil.getKeyValue("leftEye"))||list.contains(PropertiesUtil.getKeyValue("rightEye")))

					bioScan(subtype,id+IRIS_DOUBLE,identity);

				if(list.contains(PropertiesUtil.getKeyValue("rightIndex"))||list.contains(PropertiesUtil.getKeyValue("rightLittle"))||list.contains(PropertiesUtil.getKeyValue("rightRing"))||list.contains(PropertiesUtil.getKeyValue("rightMiddle")))

					bioScan(subtype,id+FINGERPRINT_SLAB_RIGHT,identity);

				if(list.contains(PropertiesUtil.getKeyValue("leftIndex"))||list.contains(PropertiesUtil.getKeyValue("leftLittle"))||list.contains(PropertiesUtil.getKeyValue("leftRing"))||list.contains(PropertiesUtil.getKeyValue("leftMiddle")))

					bioScan(subtype,id+FINGERPRINT_SLAB_LEFT,identity);

				if(list.contains(PropertiesUtil.getKeyValue("leftThumb"))||list.contains(PropertiesUtil.getKeyValue("rightThumb")))
					bioScan(subtype,id+FINGERPRINT_SLAB_THUMBS,identity);

				if(list.contains(PropertiesUtil.getKeyValue("face")))
					bioScan(subtype,id+FACE,identity);

			}
			else if(listException.contains("leftEye")||listException.contains("rightEye")||	
					listException.contains("rightIndex")||listException.contains("rightLittle")||listException.contains("rightRing")||listException.contains("rightMiddle")||
					listException.contains("leftIndex")||listException.contains("leftLittle")||listException.contains("leftRing")||listException.contains("leftMiddle")||

					listException.contains("leftThumb")||
					listException.contains("rightThumb")
					)
			{

				if(list.contains(PropertiesUtil.getKeyValue("leftEye"))||list.contains(PropertiesUtil.getKeyValue("rightEye")))
					exceptionsIrisDouble(id+IRIS_DOUBLE, identity, subtype);

				if(list.contains(PropertiesUtil.getKeyValue("rightIndex"))||list.contains(PropertiesUtil.getKeyValue("rightLittle"))||list.contains(PropertiesUtil.getKeyValue("rightRing"))||list.contains(PropertiesUtil.getKeyValue("rightMiddle")))
					exceptionsFingerPrintSlabRight(id+FINGERPRINT_SLAB_RIGHT, identity, subtype);
					

				if(list.contains(PropertiesUtil.getKeyValue("leftIndex"))||list.contains(PropertiesUtil.getKeyValue("leftLittle"))||list.contains(PropertiesUtil.getKeyValue("leftRing"))||list.contains(PropertiesUtil.getKeyValue("leftMiddle")))
					exceptionsFingerPrintSlabLeft(id+FINGERPRINT_SLAB_LEFT, identity, subtype);
					

				if(list.contains(PropertiesUtil.getKeyValue("leftThumb"))||list.contains(PropertiesUtil.getKeyValue("rightThumb")))
					exceptionsFingerPrintSlabThumbs(id+FINGERPRINT_SLAB_THUMBS, identity, subtype);
					

				if(list.contains(PropertiesUtil.getKeyValue("face")))
					bioScan(subtype,id+FACE,identity);
				
				//if(!list.contains(PropertiesUtil.getKeyValue("leftEye")) && !list.contains(PropertiesUtil.getKeyValue("rightEye")))		
				bioScan(subtype,id+EXCEPTION_PHOTO,identity);

			}
		}
		catch(Exception e)
		{
			logger.error(e.getMessage());
		}


	}


	public void newRegbioUpload(String subtype,String biostring,String id,String identity) {
		// TODO Auto-generated method stub
		try {
			if(biostring.contains(PropertiesUtil.getKeyValue("face")))
				bioScan(subtype,id+FACE,identity);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

	}

}
