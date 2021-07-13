package registrationtest.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testfx.api.FxRobot;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import registrationtest.controls.Alerts;
import registrationtest.runapplication.NewRegistrationAdultTest;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.WaitsUtil;

public class HomePage {


	private static final Logger logger = LogManager.getLogger(HomePage.class); 
	FxRobot robot;
	Stage applicationPrimaryStage;
	Scene scene;

	Button button;

	WaitsUtil waitsUtil;
	Node node;
	Alerts alerts;
	String homeimg="#homeImgView";
	String exit="#exit";
	String success="#context";

	//operationalTasks
	String syncDataImageView ="#syncDataImageView",
			downloadPreRegDataImageView="#downloadPreRegDataImageView",
			updateOperatorBiometricsImageView="updateOperatorBiometricsImageView",
			uploadPacketImageView="#uploadPacketImageView",
			remapImageView="#remapImageView",
			checkUpdatesImageView="#checkUpdatesImageView";


	//registrationTasks
	String newRegImage="#newRegImage",
			uinUpdateImage="#uinUpdateImage",
			lostUINImage="#lostUINImage";


	//eodProcesses
	String eodApprovalImageView="#eodApprovalImageView",
			reRegistrationImageView="#reRegistrationImageView",
			viewReportsImageView="#viewReportsImageView";


	public HomePage(FxRobot robot, Stage applicationPrimaryStage,Scene scene)
	{
		this.robot=robot;
		this.applicationPrimaryStage=applicationPrimaryStage;
		this.scene=scene;
		waitsUtil=new WaitsUtil(robot);

		alerts=new Alerts(robot);

	}




	public HomePage(FxRobot robot) {
		this.robot=robot;
		waitsUtil=new WaitsUtil(robot);
		waitsUtil.clickNodeAssert( homeimg);
	}



	public void clickHomeImg() {
		waitsUtil.clickNodeAssert(homeimg);
	}

	public void clickSynchronizeData() 
	{
		try
		{
			waitsUtil.clickNodeAssert(syncDataImageView);
			Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("SyncWait"))); 


			waitsUtil.clickNodeAssert(success);
			waitsUtil.clickNodeAssert( exit);

		}
		catch(Exception e)
		{logger.error(e.getMessage());
		}

	}

	public void clickdownloadPreRegDataImageView(Stage applicationPrimaryStage,Scene scene)

	{try {
		waitsUtil.clickNodeAssert( downloadPreRegDataImageView);
	}
	catch(Exception e)
	{
		logger.error(e.getMessage());
	}

	}

	public void clickupdateOperatorBiometricsImageView(Stage applicationPrimaryStage,Scene scene) 
	{
		try {
			waitsUtil.clickNodeAssert( updateOperatorBiometricsImageView);
		}
		catch(Exception e)
		{
			logger.error(e.getMessage());
		}
	}



	public UploadPacketPage clickuploadPacketImageView(Stage applicationPrimaryStage,Scene scene)
	{try {
		waitsUtil.clickNodeAssert( uploadPacketImageView);
	}
	catch(Exception e)
	{
		logger.error(e.getMessage());
	}
	return new UploadPacketPage(robot);
	}

	public void clickremapImageView(Stage applicationPrimaryStage,Scene scene)
	{try {
		waitsUtil.clickNodeAssert( remapImageView);
	}
	catch(Exception e)
	{
		logger.error(e.getMessage());
	}
	}

	public void clickcheckUpdatesImageView(Stage applicationPrimaryStage,Scene scene)
	{try {
		waitsUtil.clickNodeAssert( checkUpdatesImageView);
	}
	catch(Exception e)
	{
		logger.error(e.getMessage());
	}
	}



	public DemographicPage clickNewRegistration() 
	{try {

		waitsUtil.clickNodeAssert( newRegImage);
	}
	catch(Exception e)
	{
		logger.error(e.getMessage());
	}
	return new DemographicPage(robot);
	}

	public void clickuinUpdateImage()
	{try {
		waitsUtil.clickNodeAssert( uinUpdateImage);
	}
	catch(Exception e)
	{
		logger.error(e.getMessage());
	}
	}


	public DemographicPage clicklostUINImage() 
	{try {
		waitsUtil.clickNodeAssert( lostUINImage);
	}
	catch(Exception e)
	{
		logger.error(e.getMessage());
	}
	return new DemographicPage(robot);
	}

	public EodApprovalPage clickeodApprovalImageView(Stage applicationPrimaryStage,Scene scene) 
	{try {
		waitsUtil.clickNodeAssert( eodApprovalImageView);
	}
	catch(Exception e)
	{
		logger.error(e.getMessage());
	}
	return new EodApprovalPage(robot);
	}

	public void clickreRegistrationImageView(Stage applicationPrimaryStage,Scene scene)
	{try {
		waitsUtil.clickNodeAssert( reRegistrationImageView);
	}
	catch(Exception e)
	{
		logger.error(e.getMessage());
	}
	}

	public void clickviewReportsImageView(Stage applicationPrimaryStage,Scene scene) 
	{
		try
		{
			waitsUtil.clickNodeAssert( viewReportsImageView);
		}
		catch(Exception e)
		{
			logger.error(e.getMessage());
		}
	}





}
