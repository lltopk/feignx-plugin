package com.lyflexi.feignx.utils;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.lyflexi.feignx.enums.SpringBootClassAnnotation;
import com.lyflexi.feignx.enums.SpringCloudClassAnnotation;
import com.lyflexi.feignx.enums.SpringBootMethodAnnotation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static com.lyflexi.feignx.enums.SpringBootClassAnnotation.CONTROLLER;
import static com.lyflexi.feignx.enums.SpringBootClassAnnotation.RESTCONTROLLER;

import com.lyflexi.feignx.entity.HttpMappingInfo;
import org.apache.commons.lang3.StringUtils;

/**
 * @Author: hmly
 * @Date: 2025/3/12 19:44
 * @Project: feignx-plugin
 * @Version: 1.0.0
 * @Description: 注解解析类
 */
public class AnnotationParserUtils {

    /**
     * 寻找Restful注解
     *
     * @param method
     * @return
     */
    public static PsiAnnotation findRestfulAnnotation(PsiMethod method) {
        List<String> targetAnnotations = SpringBootMethodAnnotation.ALL_QUALIFIED_NAMES;

        //method.getModifierList()：获取方法的修饰符/注解部分
        PsiModifierList modifierList = method.getModifierList();
        if (Objects.isNull(modifierList)) {
            return null;
        }
        PsiAnnotation[] annotations = modifierList.getAnnotations();
        if (Objects.isNull(annotations)) {
            return null;
        }
        for (PsiAnnotation annotation : annotations) {
            String annoName = annotation.getQualifiedName();
            if (StringUtils.isBlank(annoName)) {
                continue;
            }
            if (targetAnnotations.contains(annoName)) {
                return annotation;
            }
        }
        //还想提升性能，可以用 psiClass.hasAnnotation() 方法，它速度更快，且内部做了缓存判断
//        for (String targetAnnotation : targetAnnotations) {
//            if (method.hasAnnotation(targetAnnotation)) {
//                return method.getAnnotation(targetAnnotation);
//            }
//        }
        return null;
    }

    /**
     * 判断当前类是controlller
     *
     * @param psiClass
     * @return
     */
    public static boolean isControllerClass(PsiClass psiClass) {
        if (psiClass == null || !psiClass.isValid()) {
            return false;
        }
        // 获取类的所有修饰符和注解
        PsiModifierList modifierList = psiClass.getModifierList();
        if (modifierList == null) {
            return false;
        }

        // 使用hasAnnotation方法替代getAnnotation
        return modifierList.hasAnnotation(SpringBootClassAnnotation.CONTROLLER.getQualifiedName()) ||
                modifierList.hasAnnotation(SpringBootClassAnnotation.RESTCONTROLLER.getQualifiedName());

//        PsiAnnotation[] annotations = psiClass.getAnnotations();
//        for (PsiAnnotation annotation : annotations) {
//            String annotationName = annotation.getQualifiedName();
//            if (annotationName != null && (annotationName.equals(CONTROLLER.getQualifiedName())
//                    || annotationName.equals(RESTCONTROLLER.getQualifiedName()))) {
//                return true;
//            }
//        }
//        return false;
    }

    /**
     * 判断当前元素是否为Controller下的方法
     *
     * @param element 元素
     * @return boolean
     */
    public static boolean isElementWithinController(PsiElement element) {
        if (element instanceof PsiClass) {
            PsiClass psiClass = (PsiClass) element;

            // 检查类上是否存在 CONTROLLER/RESTCONTROLLER 注解
            return isControllerClass(psiClass);
        }
        PsiClass type = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        return type != null && isElementWithinController(type);
    }


    /**
     * 判断当前类是否是Feign类
     *
     * @param psiClass
     * @return
     */
    public static boolean isFeignInterface(PsiClass psiClass) {
        if (psiClass == null || !psiClass.isValid()) {
            return false;
        }
        // 获取类的所有修饰符和注解
        PsiModifierList modifierList = psiClass.getModifierList();
        if (modifierList == null) {
            return false;
        }

        // 使用hasAnnotation方法替代getAnnotation
        return modifierList.hasAnnotation(SpringCloudClassAnnotation.FEIGNCLIENT.getQualifiedName());

    }

    /**
     * 元素是否为FeignClient下的方法
     * 当传入的是PsiMethod，则该方法失效
     * @param element 元素
     * @return boolean
     */
//    public static boolean isElementWithinFeign(PsiElement element) {
//        if (element instanceof PsiClass && ((PsiClass) element).isInterface()) {
//            PsiClass psiClass = (PsiClass) element;
//
//            // 检查类上是否存在 FeignClient 注解
//            PsiAnnotation feignAnnotation = psiClass.getAnnotation("org.springframework.cloud.openfeign.FeignClient");
//            if (feignAnnotation != null) {
//                return true;
//            }
//        }
//        PsiClass type = PsiTreeUtil.getParentOfType(element, PsiClass.class);
//        return type != null && isElementWithinFeign(type);
//    }

    /**
     * 元素是否为FeignClient下的方法
     * <p>
     * 更保险的方式是
     * 当传进去的是 PsiMethod，需要手动判断所有的getParentOfType是否含有注解org.springframework.cloud.openfeign.FeignClient
     *
     * @param element
     * @return
     */
    public static boolean isElementWithinFeign(PsiElement element) {
        PsiClass psiClass = null;

        if (element instanceof PsiClass) {
            psiClass = (PsiClass) element;
        } else {
            psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        }

        if (psiClass != null && psiClass.isInterface()) {
            PsiAnnotation[] annotations = psiClass.getModifierList().getAnnotations();
            for (PsiAnnotation annotation : annotations) {
                if (SpringCloudClassAnnotation.FEIGNCLIENT.getQualifiedName().equals(annotation.getQualifiedName())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 请求方法名 -> 字典映射，静态常量避免每次调用重复创建 HashMap
     */
    private static final Map<String, String> METHOD_NAME_MAPPINGS = new HashMap<>(4);

    static {
        METHOD_NAME_MAPPINGS.put("GET", "GET");
        METHOD_NAME_MAPPINGS.put("POST", "POST");
        METHOD_NAME_MAPPINGS.put("PUT", "PUT");
        METHOD_NAME_MAPPINGS.put("DELETE", "DELETE");
    }

    public static String getRequestMethodFromMethodName(String methodName) {
        return METHOD_NAME_MAPPINGS.getOrDefault(methodName, "REQUEST");
    }

    /**
     * 获得价值
     * 路径：类文件接口路径+方法接口路径
     *
     * @param httpMappingInfo 控制器信息
     * @param method          方法
     * @param annotation      注释
     * @return {@link HttpMappingInfo}
     */
    public static HttpMappingInfo getValue(PsiAnnotation annotation, HttpMappingInfo httpMappingInfo, PsiMethod method) {
        String path = getValueFromRestful(annotation);
        httpMappingInfo.setPath(httpMappingInfo.getPath() + path);
        extractSwaggerInfo(method, httpMappingInfo);
        return httpMappingInfo;
    }

    public static String getValueFromRestful(PsiAnnotation annotation) {
        PsiAnnotationParameterList parameterList = annotation.getParameterList();
        PsiNameValuePair[] attributes = parameterList.getAttributes();
        for (PsiNameValuePair attribute : attributes) {
            String attributeName = attribute.getAttributeName();
            if ("value".equals(attributeName) || "path".equals(attributeName)) {
                PsiAnnotationMemberValue attributeValue = attribute.getValue();
                if (attributeValue instanceof PsiLiteralExpression) {
                    Object value = ((PsiLiteralExpression) attributeValue).getValue();
                    if (value instanceof String) {
                        return ((String) value).startsWith("/") ? (String) value : "/" + value;
                    }
                }
                else if (attributeValue instanceof PsiReferenceExpression) {
                    // 处理引用常量的情况
                    PsiElement resolvedElement = ((PsiReferenceExpression) attributeValue).resolve();
                    if (resolvedElement instanceof PsiField) {
                        PsiField field = (PsiField) resolvedElement;
                        PsiExpression initializer = field.getInitializer();
                        if (initializer instanceof PsiLiteralExpression) {
                            Object value = ((PsiLiteralExpression) initializer).getValue();
                            if (value instanceof String) {
                                return ((String) value).startsWith("/") ? (String) value : "/" + value;
                            }
                        }
                    }
                }
            }
        }
        return "";
    }

    public static void extractSwaggerInfo(PsiMethod method, HttpMappingInfo httpMappingInfo) {
        PsiModifierList methodModifierList = method.getModifierList();
        PsiAnnotation swaggerAnnotation = methodModifierList.findAnnotation("io.swagger.annotations.ApiOperation");
        if (swaggerAnnotation != null) {
            extractSwaggerValue(swaggerAnnotation, "value", httpMappingInfo::setSwaggerInfo);
            extractSwaggerValue(swaggerAnnotation, "notes", httpMappingInfo::setSwaggerNotes);
        }
    }

    public static void extractSwaggerValue(PsiAnnotation swaggerAnnotation, String attributeName, Consumer<String> setter) {
        PsiAnnotationMemberValue attributeValue = swaggerAnnotation.findAttributeValue(attributeName);
        if (attributeValue instanceof PsiLiteralExpression) {
            Object value = ((PsiLiteralExpression) attributeValue).getValue();
            if (value instanceof String) {
                setter.accept((String) value);
            }
        }
    }

    /**
     * 判断当前方法是否拥有Restful注解
     *
     * @param method
     * @return
     */
    public static boolean containsRestfulAnnotation(PsiMethod method) {
        return Objects.nonNull(findRestfulAnnotation(method));
    }
}
