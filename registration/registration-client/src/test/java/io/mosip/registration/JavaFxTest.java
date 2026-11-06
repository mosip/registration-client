package io.mosip.registration;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Assume;

import javafx.application.Platform;

public abstract class JavaFxTest {
	@BeforeClass
	public static void startToolkit() {
		Assume.assumeTrue("Run Linux JavaFX tests under xvfb-run or in a desktop session",
				!System.getProperty("os.name").equals("Linux") || System.getenv("DISPLAY") != null);
		try {
			Platform.startup(() -> Platform.setImplicitExit(false));
		} catch (IllegalStateException alreadyStarted) {
			// The other JavaFX test class can share this toolkit.
		}
	}

	protected static void onFxThread(Runnable action) throws Exception {
		FutureTask<Void> task = new FutureTask<>(action, null);
		Platform.runLater(task);
		task.get(10, TimeUnit.SECONDS);
	}
}
