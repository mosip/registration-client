package registrationtest.pages;



import java.util.concurrent.CountDownLatch;

import org.testfx.api.FxRobot;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import registrationtest.pojo.output.RID;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.WaitsUtil;

public class WebViewDocument {
    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(WebViewDocument.class);

    FxRobot robot;
    Stage applicationPrimaryStage;
    Scene scene;
    Node node;
    ImageView newRegImage;
    WebEngine webengine;
    WebView mywebview;
    String[] RID, RIDDateTime, firstName;
    String f2;
    final RID rid = new RID();
    WaitsUtil waitsUtil;
    String webView = "#webView";

    public WebViewDocument(FxRobot robot, Stage applicationPrimaryStage, Scene scene) {
        logger.info("WebViewDocument Constructor");
        this.robot = robot;
        this.applicationPrimaryStage = applicationPrimaryStage;
        this.scene = scene;
    }

    public WebViewDocument(FxRobot robot) {
        logger.info("WebViewDocument Constructor");
        this.robot = robot;
        waitsUtil = new WaitsUtil(robot);
    }

	public RID acceptPreview(String scenario) {

		CountDownLatch latch = new CountDownLatch(1);

		try {

			Platform.runLater(() -> {
				try {

					javafx.scene.web.WebView mywebview = waitsUtil.lookupById(webView);

					rid.setWebviewPreview(mywebview.getEngine().executeScript("document.body.innerHTML;"));

					String registrationID = (String) mywebview.getEngine()
							.executeScript("document.body.getElementsByTagName('td')[0].innerHTML;");

					RID = registrationID.split("<br>");

					String registrationIDDateTime;

					if (scenario.contains("update")) {
						registrationIDDateTime = (String) mywebview.getEngine()
								.executeScript("document.body.getElementsByTagName('td')[2].innerHTML;");
					} else {
						registrationIDDateTime = (String) mywebview.getEngine()
								.executeScript("document.body.getElementsByTagName('td')[1].innerHTML;");
					}

					RIDDateTime = registrationIDDateTime.split("<br>");

				} finally {
					latch.countDown(); // ✅ signal completion
				}
			});

			// ✅ WAIT until FX thread finishes (NO sleep!)
			latch.await();

		} catch (Exception e) {
			logger.error("", e);
		}

		return new RID(RID[1], RIDDateTime[1], rid.getWebviewPreview(), rid.getWebviewPreview());
	}

    public RID getacknowledgement(String scenario) {
        try {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    javafx.scene.web.WebView mywebview = waitsUtil.lookupById(webView);

                    rid.setWebViewAck(mywebview.getEngine().executeScript("document.body.innerHTML;"));

                    String RegistrationID = (String) mywebview.getEngine()
                            .executeScript("document.body.getElementsByTagName('td')[1].innerHTML;");

                    RID = RegistrationID.split("<br>");

                    String RegistrationIDDateTime;
                    if (scenario.contains("update")) {
                        RegistrationIDDateTime = (String) mywebview.getEngine()
                                .executeScript("document.body.getElementsByTagName('td')[2].innerHTML;");
                    } else {
                        RegistrationIDDateTime = (String) mywebview.getEngine()
                                .executeScript("document.body.getElementsByTagName('td')[1].innerHTML;");
                    }
                    System.out.println("*****************************");
                    System.out.println(RID[0] + "=" + RID[1]);
                    System.out.println(RIDDateTime[0] + "=" + RIDDateTime[1]);

                    System.out.println("*****************************");
                }
            });

        }  catch (Exception e) {
        	logger.error("", e);
		}
        return new RID(RID[1], RIDDateTime[1], rid.getWebViewAck(), rid.getWebViewAck());

    }

}
