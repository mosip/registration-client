package io.mosip.registration.update;

import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.HMACUtils2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ManifestCreator {

    private static final Logger logger = LoggerFactory.getLogger(ManifestCreator.class);
    private static final String MANIFEST_FILE_NAME = "MANIFEST.MF";

    private static final Manifest manifest = new Manifest();

    public static void main(String[] args) {
        String version = args[0];
        String libraryFolderPath = args[1];
        String targetPath = args[2];

        try {
            File libFolder = new File(libraryFolderPath);
            if(libFolder.exists() && libFolder.isDirectory()) {
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, version);
                for(File file : libFolder.listFiles()) {
                    addEntryInManifest(file);
                }
                manifest.write(new FileOutputStream(targetPath + File.separator + MANIFEST_FILE_NAME));
                System.out.println("Created " + MANIFEST_FILE_NAME);
            }

        } catch (Throwable e) {
            logger.error("Failed to create the manifest", e);
        }
    }

    private static void addEntryInManifest(File file) throws Exception {
        String hashText = HMACUtils2.digestAsPlainText(FileUtils.readFileToByteArray(file));
        Attributes attribute = new Attributes();
        attribute.put(Attributes.Name.CONTENT_TYPE, hashText);
        manifest.getEntries().put(file.getName(), attribute);
    }
}
