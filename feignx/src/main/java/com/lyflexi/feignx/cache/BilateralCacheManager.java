package com.lyflexi.feignx.cache;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.lyflexi.feignx.entity.HttpMappingInfo;
import com.lyflexi.feignx.enums.SpringBootMethodAnnotation;
import com.lyflexi.feignx.enums.SpringCloudClassAnnotation;
import com.lyflexi.feignx.utils.AnnotationParserUtils;
import com.lyflexi.feignx.utils.ControllerClassScanUtils;
import com.lyflexi.feignx.utils.FeignClassScanUtils;
import com.lyflexi.feignx.utils.ProjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * @Description: 双边缓存管理器
 * @Author: lyflexi
 * @project: feignx-plugin
 * @Date: 2024/10/18 14:52
 */

public class BilateralCacheManager {

    private BilateralCacheManager() {
    }

    // 缓存controller接口数据
    // <projectid, <classpath+methodname, HttpMappingInfo>>
    private static Map<String, Map<String, HttpMappingInfo>> projectControllerCacheMap = new HashMap<>();
    // 缓存Feign接口数据
    // <projectid, <classpath+methodname, HttpMappingInfo>>
    private static Map<String, Map<String, HttpMappingInfo>> projectFeignCacheMap = new HashMap<>();

    //清除所有打开项目的所有缓存
    public static void clear() {
        Project[] openProjects = ProjectUtils.getOpenProjects();
        for (Project project : openProjects) {
            // 扫描项目中的Java源文件
            BilateralCacheManager.initControllerCaches(project, null);
            BilateralCacheManager.initFeignCaches(project, null);
        }
    }

    /**
     * 清除指定项目的所有缓存
     *
     * @param project
     */
    public static void clear(Project project) {
        String projectId = project.getBasePath();
        projectControllerCacheMap.remove(projectId);
        projectFeignCacheMap.remove(projectId);
    }

    /**
     * 清除指定项目的Feign接口缓存
     *
     * @param project
     */
    public static void clearFeignCache(Project project) {
        BilateralCacheManager.initFeignCaches(project, null);
    }

    /**
     * 清除指定项目的Controller缓存
     *
     * @param project
     */
    public static void clearControllerCache(Project project) {
        BilateralCacheManager.initControllerCaches(project, null);
    }


    /**
     * 获取所有的controller缓存
     *
     * @param project
     * @return
     */
    public static Map<String, HttpMappingInfo> queryControllerCaches(Project project) {
        String projectId = project.getBasePath(); // 以项目路径作为唯一标识符
        return projectControllerCacheMap.get(projectId);
    }

    /**
     * 获取所有的feign缓存
     *
     * @param project
     * @return
     */
    public static Map<String, HttpMappingInfo> queryFeignCaches(Project project) {
        String projectId = project.getBasePath(); // 以项目路径作为唯一标识符
        return projectFeignCacheMap.get(projectId);
    }

    /**
     * 初始化controller缓存
     *
     * @param project
     * @param controllerCaches
     */
    public static void initControllerCaches(Project project, List<HttpMappingInfo> controllerCaches) {
        String projectId = project.getBasePath(); // 以项目路径作为唯一标识符
        Map<String, HttpMappingInfo> qualifier2Info = projectControllerCacheMap.get(projectId);

        if (Objects.isNull(qualifier2Info) || qualifier2Info.isEmpty()){
            qualifier2Info = new HashMap();
            projectControllerCacheMap.put(projectId, qualifier2Info);
        }
        for (HttpMappingInfo controller : controllerCaches) {
            String qualifier = buildKey(controller.getPsiMethod());
            qualifier2Info.put(qualifier, controller);
        }
    }

    /**
     * 初始化feign缓存
     *
     * @param project
     * @param feignCaches
     */
    public static void initFeignCaches(Project project, List<HttpMappingInfo> feignCaches) {
        String projectId = project.getBasePath(); // 以项目路径作为唯一标识符
        Map<String, HttpMappingInfo> qualifier2Info = projectFeignCacheMap.get(projectId);
        if (Objects.isNull(qualifier2Info) || qualifier2Info.isEmpty()){
            qualifier2Info = new HashMap();
            projectFeignCacheMap.put(projectId, qualifier2Info);
        }
        for (HttpMappingInfo feign : feignCaches) {
            String qualifier = buildKey(feign.getPsiMethod());
            qualifier2Info.put(qualifier, feign);
        }
    }

    /**
     * 设置当前的feign方法的缓存,注意防止NPE
     *
     * @param feignMethod
     */
    public static HttpMappingInfo setFeignCache(PsiMethod feignMethod) {
        Project project = feignMethod.getProject();

        String basePath = project.getBasePath();
        //此时有可能先于feign全扫描，所以feign缓存有可能为空
        Map<String, HttpMappingInfo> qualifier2Info = projectFeignCacheMap.get(basePath);
        //下面防空NPE
        if (Objects.isNull(qualifier2Info) || qualifier2Info.isEmpty()){
            qualifier2Info = new HashMap();
            projectFeignCacheMap.put(basePath, qualifier2Info);
        }
        String qualifier = buildKey(feignMethod);
        HttpMappingInfo feignInfo = null;
        //在用户打注释/***/期间，psiMethod会有一瞬间不再拥有注解，此时HttpMappingInfo.of将返回为空, 注意避免HashMap的value为空的情况
        if (Objects.nonNull(feignInfo = FeignClassScanUtils.feignOfPsiMethod(feignMethod.getContainingClass(),feignMethod))){
            qualifier2Info.put(qualifier, feignInfo);
        }
        return feignInfo;
    }

    /**
     * 获取或者设置某个feign方法的缓存
     *
     * @param feignMethod
     * @return
     */
    public static HttpMappingInfo getOrSetFeignCache(PsiMethod feignMethod) {
        if(!AnnotationParserUtils.containsRestfulAnnotation(feignMethod)){
            return null;
        }
        String basePath = feignMethod.getProject().getBasePath();
        Map<String, HttpMappingInfo> qualifier2Info = projectFeignCacheMap.get(basePath);
        //下面防空NPE
        if (Objects.isNull(qualifier2Info) || qualifier2Info.isEmpty()){
            qualifier2Info = new HashMap();
            projectFeignCacheMap.put(basePath, qualifier2Info);
        }
        String qualifier = buildKey(feignMethod);
        if (Objects.isNull(qualifier2Info.get(qualifier))) {
            setFeignCache(feignMethod);
        }
        return qualifier2Info.get(qualifier);
    }
    /**
     * 获取或者设置某个controller方法的缓存
     *
     * @param controllerMethod
     * @return
     */
    public static HttpMappingInfo getOrSetControllerCache(PsiMethod controllerMethod) {
        if(!AnnotationParserUtils.containsRestfulAnnotation(controllerMethod)){
            return null;
        }
        String basePath = controllerMethod.getProject().getBasePath();
        Map<String, HttpMappingInfo> qualifier2Info = projectControllerCacheMap.get(basePath);
        //下面防空NPE
        if (Objects.isNull(qualifier2Info) || qualifier2Info.isEmpty()){
            qualifier2Info = new HashMap();
            projectFeignCacheMap.put(basePath, qualifier2Info);
        }
        String qualifier = buildKey(controllerMethod);
        if (Objects.isNull(qualifier2Info.get(qualifier))) {
            setControllerCache(controllerMethod);
        }
        return qualifier2Info.get(qualifier);
    }

    /**
     * 设置当前的controller方法的接口缓存,注意防止NPE
     *
     * @param controllerMethod
     */
    public static HttpMappingInfo setControllerCache(PsiMethod controllerMethod) {
        Project project = controllerMethod.getProject();

        String basePath = project.getBasePath();
        //此时有可能先于controller全扫描，所以controller缓存有可能为空
        Map<String, HttpMappingInfo> qualifier2Info = projectControllerCacheMap.get(basePath);
        //下面防空NPE
        if (Objects.isNull(qualifier2Info) || qualifier2Info.isEmpty()){
            qualifier2Info = new HashMap();
            projectControllerCacheMap.put(basePath, qualifier2Info);
        }
        String qualifier = buildKey(controllerMethod);
        //在用户打注释/***/期间，psiMethod会有一瞬间不再拥有注解，此时HttpMappingInfo.of将返回为空, 注意避免HashMap的value为空的情况
        HttpMappingInfo controllerInfo = null;
        if (Objects.nonNull(controllerInfo = ControllerClassScanUtils.controllerOfPsiMethod(controllerMethod.getContainingClass(),project,controllerMethod))){
            qualifier2Info.put(qualifier, controllerInfo);
        }
        return controllerInfo;
    }


    // -------------------- Key：Qualifier，即 类路径+方法名--------------------

    @NotNull
    private static String buildKey(PsiMethod method) {
        if (method.getContainingClass() == null || method.getContainingClass().getQualifiedName() == null) {
            return method.getName(); // 退而求其次
        }
        return method.getContainingClass().getQualifiedName() + method.getName();
    }

    @NotNull
    private static String getProjectId(Project project) {
        String path = project.getBasePath();
        return path != null ? path : String.valueOf(project.hashCode());
    }

    /**
     * 为了支持用户对当前feign接口更新，无论缓存是否存在，设置或者覆盖缓存
     * @param psiMethod
     *
     */
    public static HttpMappingInfo setOrCoverFeignCache(PsiMethod psiMethod) {
        return setFeignCache(psiMethod);
    }
    /**
     * 为了支持用户对当前controller接口更新，无论缓存是否存在，设置或者覆盖缓存
     * @param psiMethod
     *
     */
    public static HttpMappingInfo setOrCoverControllerCache(PsiMethod psiMethod) {
        return setControllerCache(psiMethod);
    }


}