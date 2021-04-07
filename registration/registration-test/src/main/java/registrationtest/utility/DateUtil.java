package registrationtest.utility;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {

	public static String getDateTime()
	  {
	
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
	   LocalDateTime now = LocalDateTime.now();
	   return dtf.format(now);
	  }
}
