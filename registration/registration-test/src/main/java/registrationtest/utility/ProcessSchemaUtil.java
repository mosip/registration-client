package registrationtest.utility;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import registrationtest.pojo.schema.Root;
import registrationtest.pojo.schema.Schema;
import registrationtest.pojo.schema.Screens;

/**
 * Resolves field identifiers from the process definition JSON files
 * (e.g. newProcess.json, updateProcess.json) instead of hardcoding them.
 */
public class ProcessSchemaUtil {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ProcessSchemaUtil.class);

    private static final Map<String, Root> processCache = new ConcurrentHashMap<>();

    private ProcessSchemaUtil() {
    }

    /**
     * Returns the identity schema field id for a document subType (POI, POA, POB, etc.)
     * as defined in the process definition for the given flow.
     */
    public static String findDocumentFieldId(String flow, String docSubType) {
        if (flow == null || docSubType == null) {
            return null;
        }
        for (Schema field : getDocumentFields(flow)) {
            if (docSubType.equalsIgnoreCase(field.getSubType())) {
                return field.getId();
            }
        }
        return null;
    }

    /**
     * Returns all document field ids defined in the process definition for the given flow.
     */
    public static List<String> getDocumentFieldIds(String flow) {
        List<String> fieldIds = new ArrayList<>();
        for (Schema field : getDocumentFields(flow)) {
            fieldIds.add(field.getId());
        }
        return fieldIds;
    }

    /**
     * Returns a map of document subType (POI, POA, ...) to identity schema field id
     * (proofOfIdentity, proofOfAddress, ...) for the given flow.
     */
    public static Map<String, String> getDocumentSubTypeToFieldIdMap(String flow) {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (Schema field : getDocumentFields(flow)) {
            if (field.getSubType() != null) {
                mapping.put(field.getSubType(), field.getId());
            }
        }
        return mapping;
    }

    /**
     * Returns all document subType codes (POI, POA, POB, etc.) defined in the process.
     */
    public static List<String> getDocumentSubTypes(String flow) {
        List<String> subTypes = new ArrayList<>();
        for (Schema field : getDocumentFields(flow)) {
            if (field.getSubType() != null) {
                subTypes.add(field.getSubType());
            }
        }
        return subTypes;
    }

    private static List<Schema> getDocumentFields(String flow) {
        Root process = loadProcess(flow);
        if (process == null || process.getScreens() == null) {
            return Collections.emptyList();
        }
        List<Schema> documentFields = new ArrayList<>();
        for (Screens screen : process.getScreens()) {
            if (screen.getFields() == null) {
                continue;
            }
            for (Schema field : screen.getFields()) {
                if (isDocumentField(field)) {
                    documentFields.add(field);
                }
            }
        }
        return documentFields;
    }

    private static boolean isDocumentField(Schema field) {
        return "fileupload".equalsIgnoreCase(field.getControlType())
                || (field.getType() != null && field.getType().contains("documentType"));
    }

    private static Root loadProcess(String flow) {
        String flowKey = flow.toLowerCase();
        return processCache.computeIfAbsent(flowKey, key -> {
            try {
                String path = System.getProperty("user.dir") + File.separator + key + "Process.json";
                String json = Files.readString(Paths.get(path));
                return JsonUtil.convertJsonintoJava(json, Root.class);
            } catch (Exception e) {
                logger.error("Failed to load process definition for flow: {}", flow, e);
                return null;
            }
        });
    }
}
