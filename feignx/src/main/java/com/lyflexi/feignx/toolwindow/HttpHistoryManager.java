package com.lyflexi.feignx.toolwindow;

import com.intellij.openapi.project.Project;
import com.lyflexi.feignx.utils.StringUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 历史请求记录管理器:将每次执行后的请求脚本 + 响应数据落盘到当前工程 {@code .idea/feignx-history} 目录。
 * <p>
 * 文件命名:{请求路径(净化)}_{时间戳}.txt,内部以两个分隔标记区分「请求」与「响应」两段,
 * 加载时按时间倒序返回 {@link HttpHistoryRecord} 列表。
 * <p>
 * 采用纯 java.io 读写,不经过 VFS(避免触发索引/刷新开销),仅在本插件内部消费。
 *
 * @Author: lyflexi
 * @project: feignx-plugin
 */
public final class HttpHistoryManager {

    private static final String DIR_NAME = "feignx-history";
    private static final String REQUEST_MARKER = "==== feignx-request ====";
    private static final String RESPONSE_MARKER = "==== feignx-response ====";
    private static final int MAX_NAME_LENGTH = 80;

    private final Project project;

    public HttpHistoryManager(Project project) {
        this.project = project;
    }

    private File historyDir() {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return null;
        }
        return new File(basePath, ".idea" + File.separator + DIR_NAME);
    }

    /**
     * 保存一条历史请求记录(请求脚本 + 响应文本)。
     */
    public void save(String script, String response) {
        if (StringUtil.isBlank(script)) {
            return;
        }
        File dir = historyDir();
        if (dir == null) {
            return;
        }
        if (!dir.exists() && !dir.mkdirs()) {
            return;
        }
        String path = extractRequestPath(script);
        String fileName = sanitize(path) + "_" + timestamp() + ".txt";
        File file = new File(dir, fileName);
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(REQUEST_MARKER);
            writer.newLine();
            writer.write(script);
            writer.newLine();
            writer.write(RESPONSE_MARKER);
            writer.newLine();
            writer.write(response == null ? "" : response);
        } catch (IOException ignored) {
            // 保存失败静默忽略,不影响主流程
        }
    }

    /**
     * 加载全部历史请求记录,按时间倒序。
     */
    public List<HttpHistoryRecord> load() {
        File dir = historyDir();
        if (dir == null || !dir.isDirectory()) {
            return Collections.emptyList();
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        List<HttpHistoryRecord> records = new ArrayList<>();
        for (File file : files) {
            HttpHistoryRecord record = read(file);
            if (record != null) {
                records.add(record);
            }
        }
        records.sort(Comparator.comparingLong(HttpHistoryRecord::getTimestamp).reversed());
        return records;
    }

    private HttpHistoryRecord read(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            int requestIdx = content.indexOf(REQUEST_MARKER);
            int responseIdx = content.indexOf(RESPONSE_MARKER);
            if (requestIdx < 0 || responseIdx < 0) {
                return null;
            }
            String script = content.substring(requestIdx + REQUEST_MARKER.length(), responseIdx).trim();
            String response = content.substring(responseIdx + RESPONSE_MARKER.length()).trim();
            String requestPath = extractRequestPath(script);
            return new HttpHistoryRecord(requestPath, script, response, file.lastModified());
        } catch (IOException e) {
            return null;
        }
    }

    private static String extractRequestPath(String script) {
        if (StringUtil.isBlank(script)) {
            return "unknown";
        }
        String[] lines = script.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split("\\s+", 2);
            if (parts.length >= 2) {
                return parts[1].trim();
            }
            return trimmed;
        }
        return "unknown";
    }

    private static String sanitize(String path) {
        if (StringUtil.isBlank(path)) {
            return "request";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : path.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        String result = sb.toString();
        return result.length() > MAX_NAME_LENGTH ? result.substring(0, MAX_NAME_LENGTH) : result;
    }

    private static String timestamp() {
        return new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date());
    }

    /**
     * 单条历史请求记录。
     */
    public static final class HttpHistoryRecord {
        private final String requestPath;
        private final String script;
        private final String response;
        private final long timestamp;

        private HttpHistoryRecord(String requestPath, String script, String response, long timestamp) {
            this.requestPath = requestPath;
            this.script = script;
            this.response = response;
            this.timestamp = timestamp;
        }

        public String getRequestPath() {
            return requestPath;
        }

        public String getScript() {
            return script;
        }

        public String getResponse() {
            return response;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return requestPath + "  ｜  " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
        }
    }
}
