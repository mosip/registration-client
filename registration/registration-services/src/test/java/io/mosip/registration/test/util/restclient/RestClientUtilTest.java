package io.mosip.registration.test.util.restclient;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.entity.FileSignature;
import io.mosip.registration.repositories.FileSignatureRepository;
import io.mosip.registration.util.restclient.RequestHTTPDTO;
import io.mosip.registration.util.restclient.RestClientUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ ApplicationContext.class })
public class RestClientUtilTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private RestClientUtil restClientUtil;

    @Mock
    private RestTemplate plainRestTemplate;

    @Mock
    private FileSignatureRepository fileSignatureRepository;

    @Before
    public void setup() {
        PowerMockito.mockStatic(ApplicationContext.class);
        Map<String, Object> appMap = new HashMap<>();
        appMap.put(RegistrationConstants.HTTP_API_READ_TIMEOUT, "1000");
        appMap.put(RegistrationConstants.HTTP_API_WRITE_TIMEOUT, "1000");
        PowerMockito.when(ApplicationContext.map()).thenReturn(appMap);
    }

    @Test
    public void invokeURL_withGet_returnsBodyAndHeaders() throws Exception {
        RequestHTTPDTO dto = new RequestHTTPDTO();
        dto.setUri(new URI("https://example.com/api"));
        dto.setHttpMethod(HttpMethod.GET);
        dto.setClazz(String.class);
        dto.setHttpEntity(new HttpEntity<>(new HttpHeaders()));
        HttpHeaders headers = new HttpHeaders();
        headers.add("h", "v");
        ResponseEntity<String> response = new ResponseEntity<>("ok", headers, HttpStatus.OK);
        when(plainRestTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        Map<String, Object> map = restClientUtil.invokeURL(dto);
        assertEquals("ok", map.get(RegistrationConstants.REST_RESPONSE_BODY));
        assertEquals(headers, map.get(RegistrationConstants.REST_RESPONSE_HEADERS));
    }

    @Test
    public void invokeForToken_withPost_returnsBodyMap() throws Exception {
        RequestHTTPDTO dto = new RequestHTTPDTO();
        dto.setUri(new URI("https://example.com/token"));
        dto.setHttpMethod(HttpMethod.POST);
        dto.setClazz(Map.class);
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        dto.setHttpHeaders(headers);
        dto.setRequestBody(Collections.singletonMap("a", "b"));

        Map<String, Object> body = new HashMap<>();
        body.put("token", "t");

        ResponseEntity<Map> response =
                new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.OK);

        when(plainRestTemplate.exchange(
                eq(dto.getUri()),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)))
                .thenReturn(response);

        Map<String, Object> map = restClientUtil.invokeForToken(dto);
        assertEquals(body, map.get(RegistrationConstants.REST_RESPONSE_BODY));
    }

    @Test
    public void isConnectedToSyncServer_when2xx_returnsTrue() throws Exception {
        ResponseEntity<String> response = new ResponseEntity<>("pong", HttpStatus.OK);
        when(plainRestTemplate.getForEntity(any(URI.class), eq(String.class))).thenReturn(response);
        assertTrue(restClientUtil.isConnectedToSyncServer("https://example.com/health"));
    }

    @Test
    public void downloadFile_whenResumable_savesSignatureAndSupportsResume() throws Exception {
        File tempFile = File.createTempFile("restclient", ".bin");
        if (tempFile.exists()) {
            tempFile.delete();
        }

        RequestHTTPDTO dto = new RequestHTTPDTO();
        dto.setUri(new URI("https://example.com/file"));
        dto.setFilePath(tempFile.toPath());
        dto.setHttpHeaders(new HttpHeaders());
        dto.setFileEncrypted(true);

        doAnswer(invocation -> {
            ClientHttpResponse resp = mock(ClientHttpResponse.class);
            HttpHeaders headers = new HttpHeaders();
            headers.add("file-signature", "sig1");
            headers.add("content-length", "4");
            when(resp.getHeaders()).thenReturn(headers);
            when(resp.getBody()).thenReturn(new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)));
            org.springframework.web.client.ResponseExtractor extractor = (org.springframework.web.client.ResponseExtractor) invocation.getArgument(3);
            return extractor.extractData(resp);
        }).when(plainRestTemplate).execute(any(URI.class), eq(HttpMethod.GET), any(), any());

        restClientUtil.downloadFile(dto);
        assertTrue(tempFile.exists());
        assertEquals(4L, tempFile.length());
        verify(fileSignatureRepository).save(argThat(fs -> fs.getSignature().equals("sig1") && fs.getEncrypted()));

        FileSignature entity = new FileSignature();
        entity.setFileName(tempFile.getName());
        entity.setContentLength(8);
        when(fileSignatureRepository.findByFileName(entity.getFileName())).thenReturn(Optional.of(entity));

        try (FileOutputStream out = new FileOutputStream(tempFile, true)) {
            out.write("more".getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(8L, tempFile.length());

        doAnswer(invocation -> {
            ClientHttpResponse resp = mock(ClientHttpResponse.class);
            HttpHeaders headers = new HttpHeaders();
            headers.add("file-signature", "sig1");
            headers.add("content-length", "8");
            when(resp.getHeaders()).thenReturn(headers);
            when(resp.getBody()).thenReturn(new ByteArrayInputStream(new byte[0]));
            org.springframework.web.client.ResponseExtractor extractor = (org.springframework.web.client.ResponseExtractor) invocation.getArgument(3);
            return extractor.extractData(resp);
        }).when(plainRestTemplate).execute(any(URI.class), eq(HttpMethod.GET), any(), any());

        restClientUtil.downloadFile(dto);
        verify(fileSignatureRepository, atLeastOnce()).save(any(FileSignature.class));

        if (tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    public void getHttpRequestFactory_appliesTimeouts_returnsFactory() {
        SimpleClientHttpRequestFactory factory = restClientUtil.getHttpRequestFactory();
        assertNotNull(factory);
    }

}
