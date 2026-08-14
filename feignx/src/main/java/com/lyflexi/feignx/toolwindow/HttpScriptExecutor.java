package com.lyflexi.feignx.toolwindow;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 解析并执行 HTTP 脚本(与 IntelliJ HTTP Client 兼容的简化格式)。
 * <p>
 * 脚本格式:
 * <pre>
 * METHOD http://host/path
 * Header-Name: value
 *
 * body
 * </pre>
 * 首行为请求行,其后为请求头(空行结束),空行后为请求体。
 * <p>
 * 执行在网络后台线程进行(调用方保证非 EDT),使用 {@link HttpURLConnection} 发送请求并收集
 * 状态码/响应头/响应体/耗时,返回 {@link HttpExecutionResult}。
 *
 * @Author: lyflexi
 * @project: feignx-plugin
 */
public final class HttpScriptExecutor {

    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 30000;

    private HttpScriptExecutor() {
    }

    /**
     * 执行脚本并返回结果(阻塞调用,请在后台线程执行)。
     */
    public static HttpExecutionResult execute(String script) throws Exception {
        ParsedRequest request = parse(script);
        return send(request);
    }

    private static ParsedRequest parse(String script) {
        if (script == null || script.trim().isEmpty()) {
            throw new IllegalArgumentException("HTTP 脚本为空");
        }
        String[] lines = script.split("\\r?\\n");

        int index = 0;
        while (index < lines.length && lines[index].trim().isEmpty()) {
            index++;
        }
        if (index >= lines.length) {
            throw new IllegalArgumentException("HTTP 脚本为空");
        }

        String[] requestLine = lines[index].trim().split("\\s+", 2);
        if (requestLine.length < 2) {
            throw new IllegalArgumentException("请求行格式错误: " + lines[index]);
        }
        String method = requestLine[0].trim().toUpperCase(Locale.ROOT);
        String url = requestLine[1].trim();
        index++;

        Map<String, String> headers = new LinkedHashMap<>();
        while (index < lines.length && !lines[index].trim().isEmpty()) {
            String line = lines[index];
            int colon = line.indexOf(':');
            if (colon > 0) {
                headers.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
            }
            index++;
        }
        // 跳过空行
        while (index < lines.length && lines[index].trim().isEmpty()) {
            index++;
        }

        StringBuilder body = new StringBuilder();
        for (int i = index; i < lines.length; i++) {
            if (body.length() > 0) {
                body.append('\n');
            }
            body.append(lines[i]);
        }
        return new ParsedRequest(method, url, headers, body.toString());
    }

    private static HttpExecutionResult send(ParsedRequest request) throws Exception {
        long start = System.currentTimeMillis();
        HttpURLConnection connection = (HttpURLConnection) new URL(request.url).openConnection();
        try {
            connection.setRequestMethod(request.method);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            for (Map.Entry<String, String> header : request.headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            if (hasBody(request.method) && !request.body.isEmpty()) {
                connection.setDoOutput(true);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(request.body.getBytes(StandardCharsets.UTF_8));
                }
            }

            int statusCode = connection.getResponseCode();
            String statusMessage = connection.getResponseMessage();
            Map<String, List<String>> responseHeaders = connection.getHeaderFields();
            InputStream inputStream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String responseBody = readAll(inputStream);
            long costMillis = System.currentTimeMillis() - start;

            return new HttpExecutionResult(statusCode, statusMessage, responseHeaders, responseBody, costMillis);
        } finally {
            connection.disconnect();
        }
    }

    private static boolean hasBody(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method);
    }

    private static String readAll(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }
        try (InputStream in = inputStream) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static final class ParsedRequest {
        private final String method;
        private final String url;
        private final Map<String, String> headers;
        private final String body;

        private ParsedRequest(String method, String url, Map<String, String> headers, String body) {
            this.method = method;
            this.url = url;
            this.headers = headers;
            this.body = body;
        }
    }

    /**
     * HTTP 执行结果:状态码/状态描述/响应头/响应体/耗时。
     */
    public static final class HttpExecutionResult {
        private final int statusCode;
        private final String statusMessage;
        private final Map<String, List<String>> responseHeaders;
        private final String body;
        private final long costMillis;

        private HttpExecutionResult(int statusCode, String statusMessage,
                                    Map<String, List<String>> responseHeaders, String body, long costMillis) {
            this.statusCode = statusCode;
            this.statusMessage = statusMessage;
            this.responseHeaders = responseHeaders;
            this.body = body;
            this.costMillis = costMillis;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getStatusMessage() {
            return statusMessage;
        }

        public String getBody() {
            return body;
        }

        public long getCostMillis() {
            return costMillis;
        }

        /**
         * 渲染为响应体面板展示文本:状态行 + 响应头 + 响应体。
         */
        public String render() {
            StringBuilder sb = new StringBuilder();
            sb.append(statusCode);
            if (statusMessage != null && !statusMessage.isEmpty()) {
                sb.append(' ').append(statusMessage);
            }
            sb.append("  (耗时 ").append(costMillis).append(" ms)\n\n");

            sb.append("=== 响应头 ===\n");
            if (responseHeaders != null) {
                List<String> values;
                for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
                    if (entry.getKey() == null) {
                        continue;
                    }
                    values = entry.getValue();
                    sb.append(entry.getKey()).append(": ");
                    if (values != null && !values.isEmpty()) {
                        sb.append(String.join(", ", values));
                    }
                    sb.append('\n');
                }
            }
            sb.append("\n=== 响应体 ===\n");
            sb.append(body == null || body.isEmpty() ? "(空)" : body);
            return sb.toString();
        }
    }
}
