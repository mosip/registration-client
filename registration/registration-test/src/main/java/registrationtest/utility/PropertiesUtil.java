
package registrationtest.utility;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Properties;




public class PropertiesUtil {
    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(PropertiesUtil.class);

    public static String getKeyValue(String key) throws IOException {

        String configFilePath = System.getProperty("user.dir") + System.getProperty("path.config");

        try (FileReader reader = new FileReader(configFilePath)) {
        	// create properties object
            Properties p = new Properties();

            // Add a wrapper around reader object
            p.load(reader);

            // access properties data
            return p.getProperty(key);
        }

    }
    
    public static void deleteFolder(String folderPath) {
        Path path = Paths.get(folderPath);
        try {
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                System.err.println("Failed to delete: " + p);
                                e.printStackTrace();
                            }
                        });

                System.out.println("Folder deleted successfully: " + folderPath);
            } else {
                System.out.println("Folder does not exist: " + folderPath);
            }
        } catch (IOException e) {
            System.err.println("Error deleting folder: " + folderPath);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        String value = getKeyValue("PropertyFilePath");
        System.out.println(value);
    }

}
