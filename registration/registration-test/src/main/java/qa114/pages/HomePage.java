package qa114.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import qa114.controls.Alerts;
import qa114.utility.PropertiesUtil;
import qa114.utility.WaitsUtil;

public class HomePage {


	private static final Logger logger = LogManager.getLogger(HomePage.class); 
	FxRobot robot;
	Stage applicationPrimaryStage;
	Scene scene;

	Button button;

	WaitsUtil waitsUtil;
	Node node;
	Alerts alerts;
	String homeimg="#homeimg";
	String exit="#exit";
	String success="Success";
	
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
		waitsUtil.clickNodeAssert(homeimg);
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
		
		
		waitsUtil.clickNodeAssert( success);
		waitsUtil.clickNodeAssert( exit);
		
		}
		catch(Exception e)
		{logger.error(e.getMessage());
		}

	}

	public void clickdownloadPreRegDataImageView(Stage applicationPrimaryStage,Scene scene) throws InterruptedException
	{
		waitsUtil.clickNodeAssert( downloadPreRegDataImageView);

	}

	public void clickupdateOperatorBiometricsImageView(Stage applicationPrimaryStage,Scene scene) throws InterruptedException
	{

		waitsUtil.clickNodeAssert( updateOperatorBiometricsImageView);
	}



	public UploadPacketPage clickuploadPacketImageView(Stage applicationPrimaryStage,Scene scene) throws InterruptedException
	{
		waitsUtil.clickNodeAssert( uploadPacketImageView);
	return new UploadPacketPage(robot);
	}

	public void clickremapImageView(Stage applicationPrimaryStage,Scene scene) throws InterruptedException
	{
		waitsUtil.clickNodeAssert( remapImageView);
	}

	public void clickcheckUpdatesImageView(Stage applicationPrimaryStage,Scene scene) throws InterruptedException
	{
		waitsUtil.clickNodeAssert( checkUpdatesImageView);
	}



	public DemographicPage clickNewRegistration() throws InterruptedException
	{

		waitsUtil.clickNodeAssert( newRegImage);
		return new DemographicPage(robot);
	}

	public void clickuinUpdateImage(Stage applicationPrimaryStage,Scene scene) throws InterruptedException
	{
		waitsUtil.clickNodeAssert( uinUpdateImage);
	}


	public DemographicPage clicklostUINImage(Stage applicationPrimaryStage,Scene scene) throws InterruptedException
	{
		waitsUtil.clickNodeAssert( lostUINImage);
		return new DemographicPage(robot);
	}

	public EodApprovalPage clickeodApprovalImageView(Stage applicationPrimaryStage,Scene scene) throws InterruptedException
	{
		waitsUtil.clickNodeAssert( eodApprovalImageView);
		return new EodApprovalPage(robot);
	}

	public void clickreRegistrationImageView(Stage applicationPrimaryStage,Scene scene) throws InterruptedException
	{
		waitsUtil.clickNodeAssert( reRegistrationImageView);
	}

	public void clickviewReportsImageView(Stage applicationPrimaryStage,Scene scene) throws InterruptedException
	{
		waitsUtil.clickNodeAssert( viewReportsImageView);

	}





}
