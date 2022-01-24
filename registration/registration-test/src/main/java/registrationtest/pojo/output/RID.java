

package registrationtest.pojo.output;

import java.util.LinkedList;
import java.util.List;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import io.mosip.registration.dao.RegistrationDAO;

public class RID {
    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(RID.class);
    @Autowired
    private RegistrationDAO registrationDAO;
    public String rid;
    public String appidrid;

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

    public RID() {
    }

    public RID(String rid, String ridDateTime) {

        this.rid = rid;
        this.ridDateTime = ridDateTime;
    }

    public RID(String rid, String ridDateTime, Object webviewPreview, Object webViewAck) {

        this.rid = rid;
        this.ridDateTime = ridDateTime;
        this.webviewPreview = webviewPreview;
        this.webViewAck = webViewAck;

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

    public String getAppidrid(ApplicationContext applicationContext, String appid) {
        registrationDAO = applicationContext.getBean(RegistrationDAO.class);
        List<String> appidList = new LinkedList<String>();
        List<String> ridid = new LinkedList<String>();
        appidList.add(appid);
        ridid = registrationDAO.getRegistrationIds(appidList);
        System.out.println(ridid);
        appidrid = ridid.get(0);
        return appidrid;
    }

    public void setAppidrid(String appidrid) {
        this.appidrid = appidrid;
    }

}
