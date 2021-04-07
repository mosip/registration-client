package  registrationtest.runapplication;
import java.io.BufferedReader;
import java.io.InputStreamReader;


	public class StartProcess {

		private static String MIN_HEAP_SIZE = "-Xms2048m";
		private static String MAX_HEAP_SIZE = "-Xmx2048m";
		private static String WIN_CMD_TEMPLATE = "%s %s %s -Dfile.encoding=UTF-8 -cp %s/*;/* io.mosip.registration.controller.Initialization %s %s";
		
	public static void main(String args[]) {
	    try {
	       	Process process = Runtime.getRuntime()
					.exec(String.format(WIN_CMD_TEMPLATE,
							"C:\\Users\\Neeharika.Garg\\Desktop\\SANDBOX ENV\\114\\reg-client (16)\\jre\\jre\\bin\\java", 
							MAX_HEAP_SIZE,
							MIN_HEAP_SIZE,
							"C:\\Users\\Neeharika.Garg\\Desktop\\SANDBOX ENV\\114\\reg-client (16)\\lib",
							"https://sandbox.mosip.net/",
							"Y"
							));
	        
	      
	        try(BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
	            String line;

	            while ((line = input.readLine()) != null) {
	                System.out.println(line);
	            }
	        }
	        process.waitFor();
	    } catch (Exception err) {
	        err.printStackTrace();
	    }
	  }
	}
// 	"C:\Users\Neeharika.Garg\Desktop\SANDBOX ENV\114\reg-client (16)\jre\jre\bin\java" -Xms2048m -Xmx2048m -Dfile.encoding=UTF-8 -cp "C:\\Users\\Neeharika.Garg\\Desktop\\SANDBOX ENV\\114\\reg-client (16)\\lib\\*" io.mosip.registration.controller.Initialization