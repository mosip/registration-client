package registrationtest.utility;


import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

import static org.awaitility.Awaitility.*;

import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.awaitility.pollinterval.FibonacciPollInterval;
import org.hamcrest.Matchers;
import org.testfx.api.FxRobot;
import org.testfx.service.support.Capture;
import org.testfx.util.DebugUtils;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import registrationtest.runapplication.NewRegistrationAdultTest;
import org.apache.log4j.BasicConfigurator;  
import org.apache.log4j.LogManager;  
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.assertj.core.internal.bytebuddy.matcher.VisibilityMatcher;
import org.testfx.util.WaitForAsyncUtils;

import static org.testfx.assertions.api.Assertions.assertThat;

/**
 * 
 * @author Neeharika.Garg
 *@References https://www.codota.com/code/java/classes/org.awaitility.Awaitility
 */
public class WaitsUtil {
	private static final Logger logger = LogManager.getLogger(WaitsUtil.class);  

	Node node;
	FxRobot robot;

	public WaitsUtil(FxRobot robot) {
		this.robot=robot;
	}

	public WaitsUtil() {
		// TODO Auto-generated constructor stub
	}

	public <T extends Node> T lookupById(final String controlId) {
		
		//verifyThat(robot.lookup("nodeQuery").tryQuery().orElse(null), isNull()); 
		 // assertThat(robot.lookup("#loginScreen").tryQuery()).isPresent();

		 
		 try {
			 
			WaitForAsyncUtils.waitFor(30, TimeUnit.SECONDS, new Callable<Boolean>() {
				    @Override
				    public Boolean call() throws Exception {
				    	Boolean b=robot.lookup(controlId).query().isVisible();
				        return b ;
				    }
				});
		} catch (TimeoutException e) {
		
			logger.error(e.getMessage());
			capture();
		}
			return robot.lookup(controlId).query();
		
		
	}


	public void clickNodeAssert(String id)
	{	
		node=lookupById(id);
		
		 assertThat(robot.lookup(id).tryQuery()).isNotNull();
		 robot.moveTo(node);
		 
		 robot.clickOn(node);
				

	}



	public <T extends Node> TextField lookupByIdTextField( String controlId,FxRobot robot) {
				try {
			with()
			.dontCatchUncaughtExceptions()
			.await()
			.pollDelay(2,  TimeUnit.SECONDS)
			.atMost(60, TimeUnit.SECONDS) 
			.until(() -> (robot.lookup(controlId).queryAs(TextField.class))!= null);
		}catch(Exception e)
		{
			logger.error(e.getMessage());
			capture();


		}

		return robot.lookup(controlId).queryAs(TextField.class);
		}


	public <T extends Node> Button lookupByIdButton( String controlId,FxRobot robot) {
		try {
			
			with().pollInSameThread()
			.await()
			.atMost(60, TimeUnit.SECONDS)
			.until(() -> (robot.lookup(controlId).queryAs(Button.class))!= null);
		}catch(Exception e)
		{
			logger.error(e.getMessage());
		capture();

		}

		return robot.lookup(controlId).queryAs(Button.class);

	}

public void capture()
{	try {
	Robot rb=new Robot();
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	
	Rectangle rec=new Rectangle(0, 0, screenSize.width, screenSize.height);
	BufferedImage image=rb.createScreenCapture(rec);
	Image myImage=SwingFXUtils.toFXImage(image, null);
	String datetim=DateUtil.getDateTime();
	String fileName="Out"+datetim+".jpg";
	ImageIO.write(image, "jpg",new File(fileName));
	} catch (Exception e) {
		logger.error(e.getMessage());
		
	}
	
}
}
