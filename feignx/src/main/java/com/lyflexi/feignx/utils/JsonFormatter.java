package com.lyflexi.feignx.utils;

/**
 * 通用 JSON 美化格式化工具(无第三方依赖)。
 * <p>
 * 通过递归下降解析 JSON 文本({@code {}} 对象 / {@code []} 数组 / 字符串 / 字面量),
 * 以两空格缩进重新输出。字符串内的转义、空白等均原样保留,不做语义改写。
 * <p>
 * 约定:{@link #format(String)} 在输入不是合法 JSON 时返回 {@code null},
 * 调用方可据此决定是否回退到原文展示。
 *
 * @Author: lyflexi
 * @project: feignx-plugin
 */
public final class JsonFormatter {

    private JsonFormatter() {
    }

    /**
     * 格式化 JSON 文本。
     *
     * @param json 待格式化文本,允许前后有空白
     * @return 格式化后的 JSON;若无法识别为 JSON 或解析失败,返回 {@code null}
     */
    public static String format(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        String trimmed = json.trim();
        int start = indexOfJsonStart(trimmed);
        if (start < 0) {
            return null;
        }
        try {
            Formatter formatter = new Formatter(trimmed, start);
            formatter.parseValue(0);
            return formatter.result.toString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int indexOfJsonStart(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{' || c == '[') {
                return i;
            }
        }
        return -1;
    }

    private static String indentStr(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }

    private static final class Formatter {
        private final String s;
        private int pos;
        private final StringBuilder result = new StringBuilder();

        private Formatter(String s, int pos) {
            this.s = s;
            this.pos = pos;
        }

        private char peek() {
            if (pos >= s.length()) {
                throw new IllegalStateException("Unexpected end of JSON");
            }
            return s.charAt(pos);
        }

        private void skipWs() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        private void parseValue(int indent) {
            skipWs();
            char c = peek();
            if (c == '{') {
                parseObject(indent);
            } else if (c == '[') {
                parseArray(indent);
            } else if (c == '"') {
                result.append(parseString());
            } else {
                parseLiteral();
            }
        }

        private void parseObject(int indent) {
            result.append('{');
            pos++;
            skipWs();
            boolean first = true;
            while (pos < s.length() && peek() != '}') {
                if (!first) {
                    result.append(',');
                    skipWs();
                    if (peek() == ',') {
                        pos++;
                    }
                }
                first = false;
                result.append('\n').append(indentStr(indent + 1));
                skipWs();
                result.append(parseString());
                skipWs();
                if (pos < s.length() && peek() == ':') {
                    pos++;
                }
                skipWs();
                result.append(": ");
                parseValue(indent + 1);
                skipWs();
            }
            if (pos < s.length() && peek() == '}') {
                pos++;
            }
            if (!first) {
                result.append('\n').append(indentStr(indent));
            }
            result.append('}');
        }

        private void parseArray(int indent) {
            result.append('[');
            pos++;
            skipWs();
            boolean first = true;
            while (pos < s.length() && peek() != ']') {
                if (!first) {
                    result.append(',');
                    skipWs();
                    if (peek() == ',') {
                        pos++;
                    }
                }
                first = false;
                result.append('\n').append(indentStr(indent + 1));
                parseValue(indent + 1);
                skipWs();
            }
            if (pos < s.length() && peek() == ']') {
                pos++;
            }
            if (!first) {
                result.append('\n').append(indentStr(indent));
            }
            result.append(']');
        }

        /**
         * 解析字符串(含引号),保留内部转义字符原样。
         */
        private String parseString() {
            StringBuilder sb = new StringBuilder();
            sb.append('"');
            pos++;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '\\') {
                    sb.append(c);
                    pos++;
                    if (pos < s.length()) {
                        sb.append(s.charAt(pos));
                        pos++;
                    }
                    continue;
                }
                if (c == '"') {
                    pos++;
                    sb.append('"');
                    break;
                }
                sb.append(c);
                pos++;
            }
            return sb.toString();
        }

        /**
         * 解析数字 / 布尔 / null 等字面量(读取至分隔符或空白)。
         */
        private void parseLiteral() {
            int start = pos;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) {
                    break;
                }
                pos++;
            }
            result.append(s, start, pos);
        }
    }
}
