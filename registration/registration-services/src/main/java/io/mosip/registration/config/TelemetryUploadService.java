package io.mosip.registration.config;

import io.tus.java.client.TusClient;
import io.tus.java.client.TusUpload;
import io.tus.java.client.TusUploader;
import io.tus.java.client.TusURLMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URL;

/**
 * TelemetryUploadService — PRODUCTION SERVICE
 *
 * Reads metrics.log written by LoggingJsonMeterRegistry (MOSIP built-in)
 * and uploads it to TUSD every 15 minutes using resumable TUS protocol.
 *
 * Pipeline:
 *   LoggingJsonMeterRegistry → metrics.log → [THIS] → TUSD → Vector → Kafka
 */
@Service
@EnableScheduling
public class TelemetryUploadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryUploadService.class);

    @Value("${mosip.registration.tus.server.url:http://localhost:8080/files/}")
    private String tusServerUrl;

    @Value("${mosip.registration.tus.server.upload.chunksize:1024}")
    private int chunkSize;

    @Value("${mosip.registration.metrics.log.path:../logs/metrics.log}")
    private String metricsLogPath;

    /**
     * Scheduled job — runs every 15 minutes automatically .
     */
    @Scheduled(fixedDelay = 30000) // i was changes 15 minutes to 30 seconds for testing purpose
    public void scheduledUpload() {
        LOGGER.info("[TelemetryUploadService] Scheduled upload triggered");
        uploadFileToTusServer(new File(metricsLogPath));
    }

    /**
     * Core upload method. Can also be called directly for testing.
     * Compatible with tus-java-client 0.4.3
     *
     * @param file The metrics.log file to upload
     */
    public void uploadFileToTusServer(File file) {
        if (!file.exists() || file.length() == 0) {
            LOGGER.info("[TelemetryUploadService] No metrics file found or empty at: {}", file.getAbsolutePath());
            return;
        }

        LOGGER.info("[TelemetryUploadService] Starting upload: {} ({} bytes)", file.getName(), file.length());

        try {
            TusClient client = new TusClient();
            client.setUploadCreationURL(new URL(tusServerUrl));
            // Enables resumable uploads — resumes from last chunk if upload fails midway
            client.enableResuming(new TusURLMemoryStore());

            TusUpload upload = new TusUpload(file);
            TusUploader uploader = client.resumeOrCreateUpload(upload);
            uploader.setChunkSize(chunkSize);

            int chunks = 0;
            while (uploader.uploadChunk() > -1) {
                chunks++;
                LOGGER.debug("[TelemetryUploadService] Chunk #{} uploaded, offset: {}", chunks, uploader.getOffset());
            }
            uploader.finish();
            LOGGER.info("[TelemetryUploadService] Upload complete. Chunks: {}", chunks);

            // Delete local file after successful upload (MOSIP design)
            if (file.delete()) {
                LOGGER.info("[TelemetryUploadService] metrics.log deleted after upload");
            } else {
                LOGGER.warn("[TelemetryUploadService] Could not delete metrics.log — will retry next cycle");
            }

        } catch (Exception e) {
            // Upload failed — file kept, will retry next scheduled run
            LOGGER.error("[TelemetryUploadService] Upload failed: {}", e.getMessage(), e);
        }
    }
}







