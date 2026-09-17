package com.jdm.storage;

import com.jdm.model.DownloadMetadata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles persistence and recovery of .jdm metadata files for download tasks.
 */
public class MetadataStore {

    public static void saveMetadata(Path metadataPath, DownloadMetadata metadata) throws IOException {
        String json = toJson(metadata);
        Files.writeString(metadataPath, json, StandardCharsets.UTF_8);
    }

    public static DownloadMetadata loadMetadata(Path metadataPath) throws IOException {
        if (!Files.exists(metadataPath)) {
            return null;
        }
        String json = Files.readString(metadataPath, StandardCharsets.UTF_8);
        return parseJson(json);
    }

    public static void deleteMetadata(Path metadataPath) {
        try {
            Files.deleteIfExists(metadataPath);
        } catch (IOException ignored) {}
    }

    // --- Lightweight JSON Serializer / Parser for DownloadMetadata ---

    public static String toJson(DownloadMetadata meta) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"id\": \"").append(escape(meta.getId())).append("\",\n");
        sb.append("  \"url\": \"").append(escape(meta.getUrl())).append("\",\n");
        sb.append("  \"targetFilePath\": \"").append(escape(meta.getTargetFilePath())).append("\",\n");
        sb.append("  \"totalSize\": ").append(meta.getTotalSize()).append(",\n");
        sb.append("  \"threadCount\": ").append(meta.getThreadCount()).append(",\n");
        sb.append("  \"priority\": \"").append(escape(meta.getPriority())).append("\",\n");
        sb.append("  \"createdTime\": ").append(meta.getCreatedTime()).append(",\n");
        sb.append("  \"lastUpdatedTime\": ").append(meta.getLastUpdatedTime()).append(",\n");
        sb.append("  \"chunks\": [\n");

        List<DownloadMetadata.ChunkMetadata> chunks = meta.getChunks();
        for (int i = 0; i < chunks.size(); i++) {
            DownloadMetadata.ChunkMetadata c = chunks.get(i);
            sb.append("    {\n");
            sb.append("      \"id\": ").append(c.getId()).append(",\n");
            sb.append("      \"startByte\": ").append(c.getStartByte()).append(",\n");
            sb.append("      \"endByte\": ").append(c.getEndByte()).append(",\n");
            sb.append("      \"downloadedBytes\": ").append(c.getDownloadedBytes()).append(",\n");
            sb.append("      \"completed\": ").append(c.isCompleted()).append("\n");
            sb.append("    }").append(i < chunks.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    public static DownloadMetadata parseJson(String json) {
        DownloadMetadata meta = new DownloadMetadata();
        meta.setId(extractString(json, "id"));
        meta.setUrl(extractString(json, "url"));
        meta.setTargetFilePath(extractString(json, "targetFilePath"));
        meta.setTotalSize(extractLong(json, "totalSize", -1));
        meta.setThreadCount(extractInt(json, "threadCount", 1));
        meta.setPriority(extractString(json, "priority"));
        meta.setCreatedTime(extractLong(json, "createdTime", 0));
        meta.setLastUpdatedTime(extractLong(json, "lastUpdatedTime", 0));

        List<DownloadMetadata.ChunkMetadata> chunks = new ArrayList<>();
        int chunksIndex = json.indexOf("\"chunks\"");
        if (chunksIndex != -1) {
            int arrayStart = json.indexOf('[', chunksIndex);
            int arrayEnd = json.lastIndexOf(']');
            if (arrayStart != -1 && arrayEnd > arrayStart) {
                String arrayContent = json.substring(arrayStart + 1, arrayEnd);
                String[] objects = arrayContent.split("(?<=\\}),\\s*(?=\\{)");
                for (String objStr : objects) {
                    if (objStr.trim().startsWith("{")) {
                        DownloadMetadata.ChunkMetadata cm = new DownloadMetadata.ChunkMetadata();
                        cm.setId(extractInt(objStr, "id", 0));
                        cm.setStartByte(extractLong(objStr, "startByte", 0));
                        cm.setEndByte(extractLong(objStr, "endByte", 0));
                        cm.setDownloadedBytes(extractLong(objStr, "downloadedBytes", 0));
                        cm.setCompleted(extractBoolean(objStr, "completed", false));
                        chunks.add(cm);
                    }
                }
            }
        }
        meta.setChunks(chunks);
        return meta;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extractString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            String val = matcher.group(1);
            return val.replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return null;
    }

    private static long extractLong(String json, String key, long defaultValue) {
        String valStr = extractValue(json, key);
        if (valStr == null) return defaultValue;
        try {
            return Long.parseLong(valStr.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int extractInt(String json, String key, int defaultValue) {
        String valStr = extractValue(json, key);
        if (valStr == null) return defaultValue;
        try {
            return Integer.parseInt(valStr.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean extractBoolean(String json, String key, boolean defaultValue) {
        String valStr = extractValue(json, key);
        if (valStr == null) return defaultValue;
        return Boolean.parseBoolean(valStr.trim());
    }

    private static String extractValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*([^,\\}\\]\\r\\n]+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
}
