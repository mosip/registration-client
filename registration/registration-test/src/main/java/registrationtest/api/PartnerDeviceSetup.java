package registrationtest.api;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONObject;

import io.mosip.testrig.apirig.testrunner.BaseTestCase;
import io.mosip.testrig.apirig.utils.AdminTestUtil;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.PartnerRegistration;
import io.mosip.testrig.apirig.utils.RestClient;
import io.restassured.response.Response;

/**
 * Device-provider registration plus PMS certificate uploads via apitest-commons
 * {@link PartnerRegistration}, with a custom partner id for Mock MDS.
 * Extends {@link AdminTestUtil} so the partner registration path comes from
 * {@code config/application.properties} ({@code putPartnerRegistrationUrl}), same as commons.
 */
public final class PartnerDeviceSetup extends AdminTestUtil {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PartnerDeviceSetup.class);

    private static final String DOMAIN = "DEVICE";

    private PartnerDeviceSetup() {
    }

    /** Registers a Device_Provider partner; an already registered partner is reused. */
    public static void registerDeviceProvider(String partnerId, String emailId) {
        String path = properties.getProperty("putPartnerRegistrationUrl");
        if (path == null || path.isBlank()) {
            throw new IllegalStateException(
                    "Missing putPartnerRegistrationUrl in config/application.properties (stage via ApitestCommonsBootstrap)");
        }
        String url = BaseTestCase.ApplnURI + path;
        String token = BaseTestCase.kernelAuthLib.getTokenByRole("partner");

        HashMap<String, String> requestBody = new HashMap<>();
        requestBody.put("address", "Registration client automation");
        requestBody.put("contactNumber", "9876543210");
        requestBody.put("emailId", emailId);
        requestBody.put("organizationName", partnerId);
        requestBody.put(GlobalConstants.PARTNERID, partnerId);
        requestBody.put("partnerType", "Device_Provider");

        HashMap<String, Object> body = new HashMap<>();
        body.put("id", GlobalConstants.STRING);
        body.put(GlobalConstants.METADATA, new HashMap<>());
        body.put(GlobalConstants.REQUEST, requestBody);
        body.put(GlobalConstants.REQUESTTIME, AdminTestUtil.generateCurrentUTCTimeStamp());
        body.put(GlobalConstants.VERSION, "LTS");

        Response response = RestClient.postRequestWithCookie(url, body, MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_JSON, GlobalConstants.AUTHORIZATION, token);
        JSONObject json = new JSONObject(response.asString());
        JSONArray errors = json.optJSONArray("errors");
        if (errors == null || errors.length() == 0) {
            logger.info("Registered device provider partner {}", partnerId);
            return;
        }
        String message = errors.toString().toLowerCase();
        if (message.contains("exist") || message.contains("duplicate") || message.contains("already")
                || message.contains("pms_prt_051")) {
            logger.info("Device provider partner {} already registered: {}", partnerId, errors);
            return;
        }
        throw new IllegalStateException("Registering device provider " + partnerId + " failed: " + errors);
    }

    public static void uploadCaCertificate(String certificatePem) {
        PartnerRegistration.uploadCACertificate(certificatePem, DOMAIN);
    }

    public static void uploadIntermediateCertificate(String certificatePem) {
        PartnerRegistration.uploadIntermediateCertificate(certificatePem, DOMAIN);
    }

    /**
     * Uploads the partner certificate and returns the certificates PMS sends back (re-signed partner
     * certificate plus trust chain).
     */
    public static List<X509Certificate> uploadPartnerCertificate(String certificatePem, String partnerId) {
        JSONObject responseValue = PartnerRegistration.uploadPartnerCertificate(certificatePem, DOMAIN, partnerId);
        if (responseValue == null || !responseValue.has("signedCertificateData")) {
            throw new IllegalStateException("Uploading partner certificate for " + partnerId
                    + " failed: missing signedCertificateData");
        }
        return parseCertificates(responseValue.getString("signedCertificateData"));
    }

    private static List<X509Certificate> parseCertificates(String data) {
        try {
            List<X509Certificate> certificates = new ArrayList<>();
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            for (java.security.cert.Certificate certificate : factory
                    .generateCertificates(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)))) {
                certificates.add((X509Certificate) certificate);
            }
            return certificates;
        } catch (CertificateException e) {
            throw new IllegalStateException("Signed partner certificate returned by PMS could not be parsed", e);
        }
    }

    /** Unique email for partner registration (PMS rejects reuse across partners). */
    public static String uniqueEmail(String partnerId) {
        return "regclient." + partnerId.toLowerCase() + "." + Calendar.getInstance().getTimeInMillis() + "@mosip.test";
    }
}
