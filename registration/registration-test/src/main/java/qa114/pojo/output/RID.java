package qa114.pojo.output;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import qa114.pages.UploadPacketPage;

public class RID {
	private static final Logger logger = LogManager.getLogger(RID.class); 
	
	   public String rid;
	    public String ridDateTime;
	    public Boolean result;
	    
	    public Boolean getResult() {
			return result;
		}

		public void setResult(Boolean result) {
			this.result = result;
		}

		public RID() {}
	    
		public RID(String rid, String ridDateTime) {
			
			this.rid = rid;
			this.ridDateTime = ridDateTime;
		}
		public String getRid() {
			return rid;
		}
		public void setRid(String rid) {
			this.rid = rid;
		}
		public String getRidDateTime() {
			return ridDateTime;
		}
		public void setRidDateTime(String ridDateTime) {
			this.ridDateTime = ridDateTime;
		}
}
