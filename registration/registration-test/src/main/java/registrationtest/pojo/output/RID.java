package registrationtest.pojo.output;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import registrationtest.pages.UploadPacketPage;

public class RID {
	private static final Logger logger = LogManager.getLogger(RID.class); 
	
	   public String rid;
	   public String firstName;
	    public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
		public String ridDateTime;
	    public Boolean result;
	    public Object webviewPreview;
	    public Object webViewAck;
	    
	    
	    public Object getWebviewPreview() {
			return webviewPreview;
		}

		public void setWebviewPreview(Object object) {
			this.webviewPreview = object;
		}

		public Object getWebViewAck() {
			return webViewAck;
		}

		public void setWebViewAck(Object webViewAck) {
			this.webViewAck = webViewAck;
		}

		
		
	    
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
		
public RID(String rid, String ridDateTime,String firstName,Object webviewPreview,Object webViewAck) {
			
			this.rid = rid;
			this.ridDateTime = ridDateTime;
			this.webviewPreview=webviewPreview;
			this.webViewAck=webViewAck;
			this.firstName=firstName;
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
