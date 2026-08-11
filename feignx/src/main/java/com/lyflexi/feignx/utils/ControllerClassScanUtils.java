package com.lyflexi.feignx.utils;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.lyflexi.feignx.entity.HttpMappingInfo;
import com.lyflexi.feignx.properties.ConfigReader;
import com.lyflexi.feignx.properties.ServerParser;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static com.lyflexi.feignx.enums.SpringBootMethodAnnotation.REQUEST_MAPPING;

/**
 * @Description: controller扫描工具类
 * @Author: lyflexi
 * @project: feignx-plugin
 * @Date: 2024/10/18 14:50
 */
public class ControllerClassScanUtils {


    private static final String SPRINGBOOT_SERVER_PATH = "server.servlet.context-path";
    private static final String SPRINGMVC_PATH = "spring.mvc.servlet.path";

    private ControllerClassScanUtils() {
    }

    /**
     * 全量扫描工程中的controllerinfos
     *
     * @param project
     * @return
     */
    public static List<HttpMappingInfo> scanControllerPaths(Project project) {
        // 检查是否在 Dumb 模式下，以避免在项目构建期间执行代码
        if (DumbService.isDumb(project)) {
            return Collections.emptyList();
        }

        List<HttpMappingInfo> httpMappingInfos = new ArrayList<>();
        // 基于 IntelliJ 注解索引精确扫描 Controller 类，不再全包递归
        List<PsiClass> controllerClasses = ProjectUtils.scanAllControllerClasses(project);

        // 单次扫描内按模块缓存 server.servlet.context-path / spring.mvc.servlet.path 的解析结果，
        // 避免每个 controller 重复递归查找 resources 目录并解析配置文件（不跨调用持留）
        Map<String, String> moduleServerPrefixCache = new HashMap<>();

        //创建全部的controller信息
        for (PsiClass psiClass : controllerClasses) {
            // 校验 psiClass 的有效性
            if (null == psiClass || !psiClass.isValid()) {
                continue;
            }
            String parentPath = controllerParentPathWithModuleCache(psiClass, project, moduleServerPrefixCache);
            for (PsiMethod method : psiClass.getMethods()) {
                HttpMappingInfo httpMappingInfo = HttpMappingInfo.of(parentPath, method);
                if (httpMappingInfo != null) {
                    // 设置psi方法信息
                    httpMappingInfo.setPsiMethod(method);
                    httpMappingInfos.add(httpMappingInfo);
                }
            }
        }

        return httpMappingInfos;
    }

    /**
     * 计算controller类级前缀路径(server.context-path + mvc.servlet.path + 类级@RequestMapping)
     * 其中 server/mvc 配置按模块(moduleRoot)在单次扫描内做内存缓存，避免每个controller重复解析同一份配置文件
     */
    private static String controllerParentPathWithModuleCache(PsiClass psiClass, Project project, Map<String, String> moduleServerPrefixCache) {
        String moduleKey = ServerParser.getModuleRootPath(psiClass, project);
        String serverPrefix = moduleServerPrefixCache.computeIfAbsent(moduleKey, k ->
                extractSpringProperties(psiClass, project, SPRINGBOOT_SERVER_PATH) + extractSpringProperties(psiClass, project, SPRINGMVC_PATH));
        return serverPrefix + controllerPsiClassPath(psiClass);
    }

    /**
     * 创建出当前psiclass（controller）内的所有HttpMappingInfo
     *
     * @param psiClass
     * @param project
     * @return
     */
    public static List<HttpMappingInfo> controllersOfPsiClass(PsiClass psiClass, Project project) {
        List<HttpMappingInfo> rs = new ArrayList<>();
        if (AnnotationParserUtils.isControllerClass(psiClass)) {
            StringBuilder parentPath = new StringBuilder();
            String serverPath = extractSpringProperties(psiClass, project, SPRINGBOOT_SERVER_PATH);
            String mvcPath = extractSpringProperties(psiClass, project, SPRINGMVC_PATH);
            String controllerPath = controllerPsiClassPath(psiClass);
            parentPath.append(serverPath).append(mvcPath).append(controllerPath);
            // 解析类中的方法，提取接口路径和Swagger注解信息
            PsiMethod[] methods = psiClass.getMethods();
            for (PsiMethod method : methods) {
                HttpMappingInfo httpMappingInfo = HttpMappingInfo.of(parentPath.toString(), method);
                if (httpMappingInfo != null) {
                    // 设置psi方法信息
                    httpMappingInfo.setPsiMethod(method);
                    rs.add(httpMappingInfo);
                }
            }
        }
        return rs;
    }

    /**
     * 创建出当前psiclass（controller）内的,指定的psiMethod对应的HttpMappingInfo
     *
     * @param psiClass
     * @param project
     * @return
     */
    public static HttpMappingInfo controllerOfPsiMethod(PsiClass psiClass, Project project, PsiMethod psiMethod) {
        HttpMappingInfo httpMappingInfo = null;
        if (AnnotationParserUtils.isControllerClass(psiClass)) {
            StringBuilder parentPath = new StringBuilder();
            String serverPath = extractSpringProperties(psiClass, project, SPRINGBOOT_SERVER_PATH);
            String mvcPath = extractSpringProperties(psiClass, project, SPRINGMVC_PATH);
            String controllerPath = controllerPsiClassPath(psiClass);
            parentPath.append(serverPath).append(mvcPath).append(controllerPath);
            // 提取接口路径和Swagger注解信息
            httpMappingInfo = HttpMappingInfo.of(parentPath.toString(), psiMethod);
            if (Objects.nonNull(httpMappingInfo)) {
                // 设置psi方法信息
                httpMappingInfo.setPsiMethod(psiMethod);
            }

        }
        return httpMappingInfo;
    }

    /**
     * resolve：
     * eg.server.servlet.context-path=/hello
     * eg .spring.mvc.servlet.path=/world
     *
     * @param psiClass
     * @param project
     * @param configKey
     * @return
     */

    public static String extractSpringProperties(PsiClass psiClass, Project project, String configKey) {
        Optional<PsiDirectory> serviceModuleDirectory = ServerParser.getServiceModuleResourcesDirectory(psiClass, project);
        String propertyPath = null;

        if (serviceModuleDirectory.isPresent()) {
            // 读取 properties 文件
            Properties properties = ConfigReader.readProperties(serviceModuleDirectory.get());
            if (properties != null && properties.containsKey(configKey)) {
                propertyPath = properties.getProperty(configKey);
            }

            // 如果在 properties 文件中未找到，继续在 yml 或 yaml 文件中查找
            if (propertyPath == null) {
                Map<String, Object> yml = ConfigReader.readYmlOrYaml(serviceModuleDirectory.get());
                if (yml != null) {
                    propertyPath = extractValueFromYml(yml, configKey);
                }
            }
        }

        return propertyPath == null ? "" : propertyPath;
    }

    // 从 YAML Map 中提取目标值，支持嵌套键
    private static String extractValueFromYml(Map<String, Object> yml, String configKey) {
        String[] keys = configKey.split("\\.");
        Object value = yml;

        for (String key : keys) {
            if (value instanceof Map) {
                value = ((Map<?, ?>) value).get(key);
            } else {
                return null;
            }
        }

        return value != null ? value.toString() : null;
    }

    /**
     * 提取Controller类文件的接口路径
     *
     * @param psiClass psi类
     * @return {@link String}
     */
    public static String controllerPsiClassPath(PsiClass psiClass) {
        PsiAnnotation[] annotations = psiClass.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String annotationName = annotation.getQualifiedName();
            if (REQUEST_MAPPING.getQualifiedName().equals(annotationName)) {
                return AnnotationParserUtils.getValueFromRestful(annotation);
            }
        }
        return "";
    }


    /**
     * 当前feign，扫描待跳转的所有目标controller
     *
     * @param psiMethod psi方法
     * @return {@link List}<{@link PsiElement}>
     */
    public static List<PsiElement> process(PsiMethod psiMethod) {
        List<PsiElement> elementList = new ArrayList<>();

        // 获取当前项目
        Project project = psiMethod.getProject();

        // 直接基于当前feign方法计算其HttpMappingInfo，不再依赖双边缓存
        HttpMappingInfo feignInfo = FeignClassScanUtils.feignOfPsiMethod(psiMethod.getContainingClass(), psiMethod);
        if (Objects.isNull(feignInfo)) {
            return elementList;
        }
        String feignPath = feignInfo.getPath();

        List<HttpMappingInfo> controllerInfos = scanControllerPaths(project);

        if (controllerInfos != null) {
            // 遍历 Controller 类的所有方法
            for (HttpMappingInfo controller : controllerInfos) {
                if (match2C(controller, feignPath)) {
                    elementList.add(controller.getPsiMethod());
                }
            }
        }
        return elementList;
    }

//    private static boolean isMethodMatch(HttpMappingInfo httpMappingInfo, PsiMethod feignMethod) {
//        PsiClass psiClass = feignMethod.getContainingClass();
//        HttpMappingInfo feignInfo = JavaSourceFileUtil.extractControllerInfo(extractFeignParentPathFromClassAnnotation(psiClass), feignMethod);
//        if(feignInfo != null){
//            String path = feignInfo.getPath();
//            if(StringUtil.isNotBlank(path)){
//                return path.equals(httpMappingInfo.getPath());
//            }
//        }
//        return false;
//    }


    public static void exportToCSV(List<HttpMappingInfo> httpMappingInfos) {
        // 获取文件选择器
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("导出列表");

        // 显示文件选择器
        int result = fileChooser.showSaveDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        // 获取文件
        File file = fileChooser.getSelectedFile();

        // 创建 CSV 文件写入器
        try (FileWriter fileWriter = new FileWriter(file.getAbsolutePath() + ".csv")) {
            // 写入列头
            String[] columnNames = {"序号", "请求方法", "路径", "Swagger Info", "Swagger Notes"};
            fileWriter.write(String.join(",", columnNames) + "\n");

            // 写入列表数据
            Integer i = 0;
            for (HttpMappingInfo httpMappingInfo : httpMappingInfos) {
                i++;
                String[] data = {
                        i.toString(),
                        httpMappingInfo.getRequestMethod(),
                        httpMappingInfo.getPath(),
                        httpMappingInfo.getSwaggerInfo(),
                        httpMappingInfo.getSwaggerNotes()
                };
                fileWriter.write(String.join(",", data) + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 用当前feign接口匹配目标Controller接口
     *
     * @param controllerInfo
     * @param feignPath
     * @return
     */
    public static boolean match2C(HttpMappingInfo controllerInfo, String feignPath) {
        return Objects.equals(feignPath, controllerInfo.getPath());
    }
}