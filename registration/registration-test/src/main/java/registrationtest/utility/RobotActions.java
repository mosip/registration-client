package registrationtest.utility;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testfx.api.FxRobot;

import javafx.scene.input.KeyCode;

public class RobotActions {
	private static final Logger logger = LogManager.getLogger(RobotActions.class); 
	
	FxRobot robot;
	
	public RobotActions(FxRobot robot) {
		this.robot=robot;
	}

	public void closeWindow()
	{
		robot.press(KeyCode.ALT);
		robot.press(KeyCode.F4);
		robot.release(KeyCode.F4);
		robot.release(KeyCode.ALT);

	}
	

	public void clickWindow()
	{
	robot.press(KeyCode.SPACE).release(KeyCode.SPACE);
	}

}
