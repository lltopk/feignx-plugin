package com.lyflexi.feignx.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiUtil;
import com.lyflexi.feignx.entity.HttpMappingInfo;
import com.lyflexi.feignx.utils.AnnotationParserUtils;
import com.lyflexi.feignx.resolver.ControllerMappingResolver;
import com.lyflexi.feignx.utils.StringUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 根据选中的请求方法生成标准 HTTP 脚本模板。
 * <p>
 * 支持 GET / POST / PUT / DELETE 请求模板,并兼容三类参数:
 * 1. 请求体 {@code @RequestBody}:解析参数实体类及其字段,递归生成 JSON 模板(含嵌套对象/集合/枚举/日期等);
 * 2. 请求参数 {@code @RequestParam}:拼接为 query 串 {@code ?name=};
 * 3. 路径参数 {@code @PathVariable}:将路径中的 {@code {name}} 占位符替换为示例值。
 * <p>
 * 脚本格式(与 IntelliJ HTTP Client 一致,便于解析与执行):
 * <pre>
 * POST http://localhost:8080/api/user/save
 * Content-Type: application/json
 *
 * {
 *   "name": "string",
 *   "age": 0
 * }
 * </pre>
 * <p>
 * 注意:所有 PSI 读取必须运行在 ReadAction / EDT 内(调用方保证)。
 *
 * @Author: lyflexi
 * @project: feignx-plugin
 */
public final class HttpScriptGenerator {

    private static final String REQUEST_BODY = "org.springframework.web.bind.annotation.RequestBody";
    private static final String REQUEST_PARAM = "org.springframework.web.bind.annotation.RequestParam";
    private static final String PATH_VARIABLE = "org.springframework.web.bind.annotation.PathVariable";

    private static final String SERVER_PORT_KEY = "server.port";
    private static final String DEFAULT_PORT = "8080";

    /** 实体类字段递归解析的最大层级,防止自引用/循环引用导致无限递归 */
    private static final int MAX_DEPTH = 4;

    private HttpScriptGenerator() {
    }

    /**
     * 生成标准 HTTP 脚本模板。
     *
     * @param info 选中的请求方法信息(含路径/请求类型/PSI 方法)
     * @return 可直接展示/编辑/执行的 HTTP 脚本文本
     */
    public static String generate(HttpMappingInfo info) {
        PsiMethod method = info.getPsiMethod();
        if (method == null || !method.isValid()) {
            return "";
        }
        String httpMethod = normalizeMethod(info.getRequestMethod());
        String baseUrl = "http://localhost:" + resolveServerPort(method);
        String url = baseUrl + normalizePath(info.getPath());

        PsiParameter[] parameters = method.getParameterList().getParameters();
        Map<String, PsiType> pathVars = new LinkedHashMap<>();
        StringBuilder query = new StringBuilder();
        String body = null;

        for (PsiParameter parameter : parameters) {
            if (parameter.getAnnotation(REQUEST_BODY) != null) {
                body = buildJsonValue(parameter.getType(), 0);
            } else if (parameter.getAnnotation(PATH_VARIABLE) != null) {
                String name = annotationParamName(parameter, PATH_VARIABLE);
                if (StringUtil.isNotBlank(name)) {
                    pathVars.put(name, parameter.getType());
                }
            } else if (parameter.getAnnotation(REQUEST_PARAM) != null) {
                String name = annotationParamName(parameter, REQUEST_PARAM);
                if (StringUtil.isNotBlank(name)) {
                    appendQuery(query, name, sampleValue(parameter.getType()));
                }
            } else if (isSimpleType(parameter.getType())) {
                // 未标注注解的简单类型参数:SpringMVC 默认按 query 参数绑定
                appendQuery(query, parameter.getName(), sampleValue(parameter.getType()));
            }
        }

        for (Map.Entry<String, PsiType> entry : pathVars.entrySet()) {
            url = url.replace("{" + entry.getKey() + "}", sampleValue(entry.getValue()));
        }
        if (query.length() > 0) {
            url = url + (url.contains("?") ? "&" : "?") + query;
        }

        StringBuilder script = new StringBuilder();
        script.append(httpMethod).append(' ').append(url).append('\n');
        if (body != null) {
            script.append("Content-Type: application/json\n");
        }
        if (body != null) {
            script.append('\n').append(body);
        }
        return script.toString();
    }

    private static String normalizeMethod(String requestMethod) {
        if (StringUtil.isBlank(requestMethod) || "REQUEST".equalsIgnoreCase(requestMethod)) {
            return "GET";
        }
        return requestMethod.trim().toUpperCase();
    }

    private static String normalizePath(String path) {
        if (StringUtil.isBlank(path)) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    /**
     * 解析当前方法所属模块的 {@code server.port}:优先读模块 yml/properties 配置,
     * 未配置或非法时回退默认端口 8080。
     */
    private static String resolveServerPort(PsiMethod method) {
        PsiClass psiClass = method.getContainingClass();
        if (psiClass == null) {
            return DEFAULT_PORT;
        }
        Project project = method.getProject();
        String port = ControllerMappingResolver.extractSpringProperties(psiClass, project, SERVER_PORT_KEY);
        if (StringUtil.isNotBlank(port)) {
            String trimmed = port.trim();
            try {
                Integer.parseInt(trimmed);
                return trimmed;
            } catch (NumberFormatException ignored) {
                // 非数字端口(如占位符/范围),回退默认
            }
        }
        return DEFAULT_PORT;
    }

    private static String annotationParamName(PsiParameter parameter, String annotationQualifiedName) {
        PsiAnnotation annotation = parameter.getAnnotation(annotationQualifiedName);
        if (annotation == null) {
            return parameter.getName();
        }
        PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
        if (value == null) {
            value = annotation.findAttributeValue("name");
        }
        String resolved = AnnotationParserUtils.resolveStringValue(value);
        return StringUtil.isNotBlank(resolved) ? resolved : parameter.getName();
    }

    private static void appendQuery(StringBuilder query, String name, String value) {
        if (StringUtil.isBlank(name)) {
            return;
        }
        if (query.length() > 0) {
            query.append('&');
        }
        query.append(name).append('=').append(value);
    }

    // ------------------------------------------------------------------
    // 类型 -> 示例值 / JSON 值 构建
    // ------------------------------------------------------------------

    /**
     * 生成 JSON 值模板(带引号/结构),用于请求体。
     *
     * @param type  字段类型
     * @param level 当前嵌套层级(用于缩进与递归上限)
     */
    private static String buildJsonValue(PsiType type, int level) {
        if (type == null) {
            return "null";
        }
        if (type instanceof PsiPrimitiveType) {
            String name = type.getCanonicalText();
            if ("boolean".equals(name)) {
                return "false";
            }
            if ("char".equals(name)) {
                return "\"a\"";
            }
            if ("float".equals(name) || "double".equals(name)) {
                return "0.0";
            }
            return "0";
        }
        PsiClass psiClass = PsiUtil.resolveClassInType(type);
        if (psiClass == null) {
            return "null";
        }
        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null) {
            return "{}";
        }
        if ("java.lang.String".equals(qualifiedName) || "java.lang.Character".equals(qualifiedName)) {
            return "\"string\"";
        }
        if ("java.lang.Boolean".equals(qualifiedName)) {
            return "false";
        }
        if (isNumericClass(qualifiedName)) {
            return "0";
        }
        if (isDateClass(qualifiedName)) {
            return "\"2024-01-01 00:00:00\"";
        }
        if (psiClass.isEnum()) {
            PsiField[] fields = psiClass.getFields();
            if (fields.length > 0) {
                return "\"" + fields[0].getName() + "\"";
            }
            return "\"ENUM\"";
        }
        if (InheritanceUtil.isInheritor(psiClass, "java.util.Collection")) {
            PsiType elementType = elementType(type);
            if (elementType == null) {
                return "[]";
            }
            return "[" + buildJsonValue(elementType, level) + "]";
        }
        if (InheritanceUtil.isInheritor(psiClass, "java.util.Map")) {
            return "{\"key\": \"value\"}";
        }
        if (level >= MAX_DEPTH) {
            return "{...}";
        }
        return buildObjectJson(psiClass, level);
    }

    /**
     * 根据实体类字段构建 JSON 对象模板。
     *
     * @param psiClass 实体类
     * @param level    当前对象所在层级
     */
    private static String buildObjectJson(PsiClass psiClass, int level) {
        List<PsiField> fields = collectFields(psiClass);
        if (fields.isEmpty()) {
            return "{}";
        }
        String closePad = indent(level);
        String fieldPad = indent(level + 1);
        StringBuilder sb = new StringBuilder("{\n");
        for (int i = 0; i < fields.size(); i++) {
            PsiField field = fields.get(i);
            sb.append(fieldPad).append('"').append(field.getName()).append("\": ")
                    .append(buildJsonValue(field.getType(), level + 1));
            if (i < fields.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append(closePad).append('}');
        return sb.toString();
    }

    /**
     * 收集实体类可序列化字段(排除 static / serialVersionUID)。
     */
    private static List<PsiField> collectFields(PsiClass psiClass) {
        List<PsiField> result = new ArrayList<>();
        for (PsiField field : psiClass.getAllFields()) {
            if (field.hasModifierProperty(PsiModifier.STATIC)) {
                continue;
            }
            if ("serialVersionUID".equals(field.getName())) {
                continue;
            }
            result.add(field);
        }
        return result;
    }

    /**
     * 生成用于 URL(query/path) 的示例值(不带引号)。
     */
    private static String sampleValue(PsiType type) {
        if (type == null) {
            return "1";
        }
        if (type instanceof PsiPrimitiveType) {
            String name = type.getCanonicalText();
            if ("boolean".equals(name)) {
                return "true";
            }
            if ("char".equals(name)) {
                return "a";
            }
            if ("float".equals(name) || "double".equals(name)) {
                return "1.0";
            }
            return "1";
        }
        PsiClass psiClass = PsiUtil.resolveClassInType(type);
        if (psiClass == null) {
            return "1";
        }
        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null) {
            return "1";
        }
        if ("java.lang.String".equals(qualifiedName) || "java.lang.Character".equals(qualifiedName)) {
            return "string";
        }
        if ("java.lang.Boolean".equals(qualifiedName)) {
            return "true";
        }
        if (isNumericClass(qualifiedName)) {
            return "1";
        }
        if (isDateClass(qualifiedName)) {
            return "2024-01-01";
        }
        if (psiClass.isEnum()) {
            PsiField[] fields = psiClass.getFields();
            return fields.length > 0 ? fields[0].getName() : "enum";
        }
        return "1";
    }

    /**
     * 判断是否可直接作为 query 参数的简单类型(基本类型/包装类型/String/枚举/日期)。
     */
    private static boolean isSimpleType(PsiType type) {
        if (type instanceof PsiPrimitiveType) {
            return true;
        }
        PsiClass psiClass = PsiUtil.resolveClassInType(type);
        if (psiClass == null) {
            return false;
        }
        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null) {
            return false;
        }
        return "java.lang.String".equals(qualifiedName)
                || "java.lang.Character".equals(qualifiedName)
                || "java.lang.Boolean".equals(qualifiedName)
                || isNumericClass(qualifiedName)
                || isDateClass(qualifiedName)
                || psiClass.isEnum();
    }

    private static PsiType elementType(PsiType type) {
        if (type instanceof PsiArrayType) {
            return ((PsiArrayType) type).getComponentType();
        }
        if (type instanceof PsiClassType) {
            PsiType[] parameters = ((PsiClassType) type).getParameters();
            if (parameters.length > 0) {
                return parameters[0];
            }
        }
        return null;
    }

    private static boolean isNumericClass(String qualifiedName) {
        return "java.lang.Integer".equals(qualifiedName)
                || "java.lang.Long".equals(qualifiedName)
                || "java.lang.Short".equals(qualifiedName)
                || "java.lang.Byte".equals(qualifiedName)
                || "java.lang.Float".equals(qualifiedName)
                || "java.lang.Double".equals(qualifiedName)
                || "java.math.BigDecimal".equals(qualifiedName)
                || "java.math.BigInteger".equals(qualifiedName)
                || "java.util.concurrent.atomic.AtomicInteger".equals(qualifiedName)
                || "java.util.concurrent.atomic.AtomicLong".equals(qualifiedName);
    }

    private static boolean isDateClass(String qualifiedName) {
        return qualifiedName.startsWith("java.time.")
                || "java.util.Date".equals(qualifiedName)
                || "java.sql.Date".equals(qualifiedName)
                || "java.sql.Timestamp".equals(qualifiedName)
                || "java.sql.Time".equals(qualifiedName);
    }

    private static String indent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }
}
