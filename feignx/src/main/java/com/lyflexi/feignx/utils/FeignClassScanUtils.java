package com.lyflexi.feignx.utils;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.lyflexi.feignx.entity.HttpMappingInfo;
import com.lyflexi.feignx.enums.SpringCloudClassAnnotation;
import java.util.*;

import org.jetbrains.annotations.NotNull;

/**
 * @Author: hmly
 * @Date: 2025/3/14 20:51
 * @Project: feignx-plugin
 * @Version: 1.0.0
 * @Description: feign类扫描工具类
 */
public class FeignClassScanUtils {
    /**
     * 当前controller，扫描待跳转的所有目标Feign
     *
     * @param controllerMethod psi方法
     * @return {@link List}<{@link PsiElement}>
     */
    public static List<PsiElement> process(PsiMethod controllerMethod) {
        List<PsiElement> elementList = new ArrayList<>();
        // 获取当前项目
        Project project = controllerMethod.getProject();
        // 直接基于当前controller方法计算其HttpMappingInfo，不再依赖双边缓存
        HttpMappingInfo controllerInfo = ControllerClassScanUtils.controllerOfPsiMethod(controllerMethod.getContainingClass(), project, controllerMethod);
        if (Objects.isNull(controllerInfo)) {
            return elementList;
        }
        String path = controllerInfo.getPath();
        // 单次扫描内构建 path -> HttpMappingInfo 索引，匹配命中 O(1)
        Map<String, List<HttpMappingInfo>> feignIndex = scanFeignIndex(project);
        List<HttpMappingInfo> matchedInfos = feignIndex.get(path);
        if (matchedInfos != null) {
            for (HttpMappingInfo feignInfo : matchedInfos) {
                elementList.add(feignInfo.getPsiMethod());
            }
        }

        return elementList;
    }


    /**
     * 扫描Feign接口信息
     *
     * @param project 项目
     * @return {@link List}<{@link HttpMappingInfo}>
     */
    public static List<HttpMappingInfo> scanFeignInterfaces(Project project) {
        List<HttpMappingInfo> feignInfos = new ArrayList<>();
        for (List<HttpMappingInfo> infos : scanFeignIndex(project).values()) {
            feignInfos.addAll(infos);
        }
        return feignInfos;
    }

    /**
     * 单次扫描内构建 接口全路径 -> HttpMappingInfo 索引
     * 仅基于 IntelliJ 注解索引扫描 @FeignClient 接口，不再全包递归，也不跨调用持留任何缓存
     */
    private static Map<String, List<HttpMappingInfo>> scanFeignIndex(Project project) {
        // 检查是否在 Dumb 模式下，以避免在项目构建期间执行代码
        if (DumbService.isDumb(project)) {
            return Collections.emptyMap();
        }

        Map<String, List<HttpMappingInfo>> index = new HashMap<>();
        // 基于 IntelliJ 注解索引精确扫描 @FeignClient 接口
        List<PsiClass> feignClasses = ProjectUtils.scanAllFeignClasses(project);
        for (PsiClass psiClass : feignClasses) {
            // 校验 psiClass 的有效性
            if (null == psiClass || !psiClass.isValid()) {
                continue;
            }
            String parentPath = extractFeignParentPathFromClassAnnotation(psiClass);
            for (PsiMethod method : psiClass.getMethods()) {
                HttpMappingInfo feignInfo = HttpMappingInfo.of(parentPath, method);
                if (feignInfo != null) {
                    // 设置方法信息
                    feignInfo.setPsiMethod(method);
                    index.computeIfAbsent(feignInfo.getPath(), k -> new ArrayList<>()).add(feignInfo);
                }
            }
        }
        return index;
    }

    /**
     * 获取当前feign类中的所有方法对应的HttpMappingInfo
     *
     * @param psiClass
     * @return
     */
    public static List<HttpMappingInfo> feignsOfPsiClass(PsiClass psiClass) {
        List<HttpMappingInfo> rs = new ArrayList<>();
        if (AnnotationParserUtils.isFeignInterface(psiClass)) {
            // 解析类中的方法，提取接口路径
            PsiMethod[] methods = psiClass.getMethods();
            String parentPath = extractFeignParentPathFromClassAnnotation(psiClass);
            for (PsiMethod method : methods) {
                HttpMappingInfo feignInfo = HttpMappingInfo.of(parentPath, method);
                if (feignInfo != null) {
                    // 设置方法信息
                    feignInfo.setPsiMethod(method);
                    rs.add(feignInfo);
                }
            }
        }
        return rs;
    }

    /**
     * 获取当前feign类中的, 指定PsiMethod方法的HttpMappingInfo
     *
     * @param psiClass
     * @param method
     * @return
     */
    public static HttpMappingInfo feignOfPsiMethod(PsiClass psiClass, PsiMethod method) {
        HttpMappingInfo httpMappingInfo = null;
        if (AnnotationParserUtils.isFeignInterface(psiClass)) {
            // 解析类中的方法，提取接口路径
            String parentPath = extractFeignParentPathFromClassAnnotation(psiClass);
            httpMappingInfo = HttpMappingInfo.of(parentPath, method);
            if (Objects.nonNull(httpMappingInfo)) {
                // 设置方法信息
                httpMappingInfo.setPsiMethod(method);
            }
        }
        return httpMappingInfo;
    }

    /**
     * 提取@FeignClient path属性值
     */
    public static String extractFeignParentPathFromClassAnnotation(PsiClass psiClass) {
        PsiAnnotation annotation = psiClass.getAnnotation(SpringCloudClassAnnotation.FEIGNCLIENT.getQualifiedName());
        PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
        for (PsiNameValuePair attribute : attributes) {
            if ("path".equals(attribute.getName())) {
                // 支持字面量/单个常量引用/常量拼接,如 @FeignClient(path = Constants.PREFIX + "/user")
                String path = AnnotationParserUtils.resolveStringValue(attribute.getValue());
                if (path != null) {
                    return handlePath(path);
                }
            }
        }
        return "";
    }

    private static @NotNull String handlePath(String pathStr) {
        // @geasscai https://github.com/Halfmoonly/feignx-plugin/pull/9
        if (StringUtil.isBlank(pathStr)) {
            return "";
        }
        // @geasscai https://github.com/Halfmoonly/feignx-plugin/pull/9
        // 如果path不以/开头，添加/
        if (!pathStr.startsWith("/")) {
            pathStr = "/" + pathStr;
        }
        // @geasscai https://github.com/Halfmoonly/feignx-plugin/pull/9
        // 如果path以/结尾，去除/
        if (pathStr.endsWith("/")) {
            pathStr = pathStr.substring(0, pathStr.length() - 1);
        }
        return pathStr;
    }
}
