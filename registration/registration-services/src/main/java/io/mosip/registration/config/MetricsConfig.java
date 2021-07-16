package io.mosip.registration.config;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.metrics.DiskMetrics;
import io.mosip.registration.metrics.PacketMetrics;
import io.mosip.registration.metrics.SystemTimeMetrics;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;

import io.tus.java.client.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

@Configuration
@EnableScheduling
public class MetricsConfig {

    private static final Logger LOGGER = AppConfig.getLogger(MetricsConfig.class);
    private static final String TUS_SERVER_URL_CONFIG = "mosip.registration.tus.server.url";
    private static final String TUS_SERVER_UPLOAD_CHUNKSIZE = "mosip.registration.tus.server.upload.chunksize";

    @Bean
    public MeterRegistry getMeterRegistry() {
        LoggingMeterRegistry registry = new LoggingMeterRegistry(new LoggingRegistryConfig() {
            @Override
            public Duration step() {
                return Duration.ofSeconds(60);
            }

            @Override
            public String get(String key) {
                return null;
            }
        }, Clock.SYSTEM);
        registry.config().commonTags("machine", RegistrationSystemPropertiesChecker.getMachineId());
        return registry;
    }

    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics(MeterRegistry meterRegistry) {
        JvmMemoryMetrics jvmMemoryMetrics = new JvmMemoryMetrics();
        jvmMemoryMetrics.bindTo(meterRegistry);
        return jvmMemoryMetrics;
    }

    @Bean
    public JvmThreadMetrics jvmThreadMetrics(MeterRegistry meterRegistry) {
        JvmThreadMetrics jvmThreadMetrics = new JvmThreadMetrics();
        jvmThreadMetrics.bindTo(meterRegistry);
        return jvmThreadMetrics;
    }

    @Bean
    public JvmGcMetrics jvmGcMetrics(MeterRegistry meterRegistry) {
        JvmGcMetrics jvmGcMetrics = new JvmGcMetrics();
        jvmGcMetrics.bindTo(meterRegistry);
        return jvmGcMetrics;
    }

    @Bean
    public JvmHeapPressureMetrics jvmHeapPressureMetrics(MeterRegistry meterRegistry) {
        JvmHeapPressureMetrics jvmHeapPressureMetrics = new JvmHeapPressureMetrics();
        jvmHeapPressureMetrics.bindTo(meterRegistry);
        return jvmHeapPressureMetrics;
    }

    @Bean
    public ProcessorMetrics processorMetrics(MeterRegistry meterRegistry) {
        ProcessorMetrics processorMetrics = new ProcessorMetrics();
        processorMetrics.bindTo(meterRegistry);
        return processorMetrics;
    }

    @Bean
    public ClassLoaderMetrics classLoaderMetrics(MeterRegistry meterRegistry) {
        ClassLoaderMetrics classLoaderMetrics = new ClassLoaderMetrics();
        classLoaderMetrics.bindTo(meterRegistry);
        return classLoaderMetrics;
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        return new TimedAspect(meterRegistry);
    }

    @Bean
    public CountedAspect countedAspect(MeterRegistry meterRegistry) {
        return new CountedAspect(meterRegistry);
    }

    @Bean
    public DiskMetrics diskMetrics(MeterRegistry meterRegistry) {
        DiskMetrics diskMetrics = new DiskMetrics();
        diskMetrics.bindTo(meterRegistry);
        return diskMetrics;
    }

    @Bean
    public SystemTimeMetrics systemTimeMetrics(MeterRegistry meterRegistry) {
        SystemTimeMetrics systemTimeMetrics = new SystemTimeMetrics();
        systemTimeMetrics.bindTo(meterRegistry);
        return systemTimeMetrics;
    }

    @Bean
    public PacketMetrics packetMetrics(MeterRegistry meterRegistry, ApplicationContext applicationContext) {
        PacketMetrics packetMetrics = new PacketMetrics(applicationContext);
        packetMetrics.bindTo(meterRegistry);
        return packetMetrics;
    }

    @Scheduled(initialDelay = 15*60*1000, fixedDelay =  15*60*1000)
    public void exportMetrics() {

        try {
            // Create a new TusClient instance
            TusClient client = new TusClient();
            // Configure tus HTTP endpoint. This URL will be used for creating new uploads
            // using the Creation extension
            String url = (String) io.mosip.registration.context.ApplicationContext.map()
                    .getOrDefault(TUS_SERVER_URL_CONFIG,"http://localhost:8080/files");
            int chunkSize = Integer.valueOf((String)io.mosip.registration.context.ApplicationContext.map()
                    .getOrDefault(TUS_SERVER_UPLOAD_CHUNKSIZE,"1024"));
            client.setUploadCreationURL(new URL(url));

            // Enable resumable uploads by storing the upload URL in memory
            client.enableResuming(new TusURLMemoryStore());

            // Open a file using which we will then create a TusUpload. If you do not have
            // a File object, you can manually construct a TusUpload using an InputStream.
            // See the documentation for more information.
            final TusUpload upload = new TusUpload(getFile());

            LOGGER.info("Starting upload...{}", upload.getMetadata());

            // We wrap our uploading code in the TusExecutor class which will automatically catch
            // exceptions and issue retries with small delays between them and take fully
            // advantage of tus' resumability to offer more reliability.
            // This step is optional but highly recommended.
            TusExecutor executor = new TusExecutor() {
                @Override
                protected void makeAttempt() throws ProtocolException, IOException {
                    // First try to resume an upload. If that's not possible we will create a new
                    // upload and get a TusUploader in return. This class is responsible for opening
                    // a connection to the remote server and doing the uploading.
                    TusUploader uploader = client.resumeOrCreateUpload(upload);

                    // Alternatively, if your tus server does not support the Creation extension
                    // and you obtained an upload URL from another service, you can instruct
                    // tus-java-client to upload to a specific URL. Please note that this is usually
                    // _not_ necessary and only if the tus server does not support the Creation
                    // extension. The Vimeo API would be an example where this method is needed.
                    // TusUploader uploader = client.beginOrResumeUploadFromURL(upload, new URL("https://tus.server.net/files/my_file"));

                    // Upload the file in chunks of 1KB sizes.
                    uploader.setChunkSize(chunkSize);

                    // Upload the file as long as data is available. Once the
                    // file has been fully uploaded the method will return -1
                    do {
                        // Calculate the progress using the total size of the uploading file and
                        // the current offset.
                        long totalBytes = upload.getSize();
                        long bytesUploaded = uploader.getOffset();
                        double progress = (double) bytesUploaded / totalBytes * 100;

                        LOGGER.info("Upload at {}", progress);
                    } while(uploader.uploadChunk() > -1);

                    // Allow the HTTP connection to be closed and cleaned up
                    uploader.finish();

                    LOGGER.info("Upload finished.");
                    LOGGER.info("Upload available at: {}", uploader.getUploadURL().toString());

                    //TODO delete archive log file
                }
            };
            executor.makeAttempts();
        } catch (Exception e) {
            LOGGER.error("Error uploading metrics", e);
        }
    }

    private File getFile() throws Exception {
        Optional<Path> result = Files.list(Path.of(System.getProperty("user.dir"), "logs"))
                    .filter(s -> s.toFile().getName().startsWith("metrics-archive"))
                    .sorted()
                    .findFirst();

        if(result.isPresent())
            return result.get().toFile();

        throw new Exception("*** No metrics archive files to sync ***");
    }

}
