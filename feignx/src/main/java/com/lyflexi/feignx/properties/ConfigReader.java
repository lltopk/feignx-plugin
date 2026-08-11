package com.lyflexi.feignx.properties;


import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.lyflexi.feignx.utils.CollectionUtil;
import com.lyflexi.feignx.utils.StringUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * @Description: 项目初始化阶段配置文件读取与解析
 * @Author: lyflexi
 * @project: feignx-plugin
 * @Date: 2024/11/3 15:01 
 */

public class ConfigReader {
    //支持配置文件解析1.server.servlet.context-path and 2.spring.mvc.servlet.path仅存在于bootstrap.yml
    private static final String PROPERTIES_FILE_NAME = "application.properties";
    private static final String PROPERTIES_BOOTSTRAP_FILE_NAME = "bootstrap.properties";
    private static final String YML_FILE_NAME = "application.yml";
    private static final String YAML_FILE_NAME = "application.yaml";

    //支持nacos场景，1.server.servlet.context-path and 2.spring.mvc.servlet.path仅存在于bootstrap.yml
    // @geasscai https://github.com/Halfmoonly/feignx-plugin/pull/9
    private static final String YML_BOOTSTRAP_FILE_NAME = "bootstrap.yml";
    //支持nacos场景，1.server.servlet.context-path and 2.spring.mvc.servlet.path仅存在于bootstrap.yml
    // @geasscai https://github.com/Halfmoonly/feignx-plugin/pull/9
    private static final String YAML_BOOTSTRAP_FILE_NAME = "bootstrap.yaml";

    // 支持 spring.profiles.active 指定的 profile 专属配置文件，如 application-local.yml (Issue #17)
    private static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";
    private static final String APPLICATION_PROFILE_PREFIX = "application-";
    private static final String BOOTSTRAP_PROFILE_PREFIX = "bootstrap-";


    /**
     * 读取properties(含 profile 专属配置文件,如 application-local.properties)
     * @param moduleDirectory
     * @return
     */
    public static Properties readProperties(PsiDirectory moduleDirectory) {
        // 创建一个新的 Properties 对象用于存储整合后的结果
        Properties mergedProperties = new Properties();

        // 合并基础配置文件，后面的覆盖前面的
        mergedProperties.putAll(readPropertiesFromFile(moduleDirectory, PROPERTIES_FILE_NAME));
        mergedProperties.putAll(readPropertiesFromFile(moduleDirectory, PROPERTIES_BOOTSTRAP_FILE_NAME));

        // 合并 profile 专属配置文件，覆盖基础配置
        for (String profile : resolveActiveProfiles(moduleDirectory)) {
            mergedProperties.putAll(readPropertiesFromFile(moduleDirectory, APPLICATION_PROFILE_PREFIX + profile + ".properties"));
            mergedProperties.putAll(readPropertiesFromFile(moduleDirectory, BOOTSTRAP_PROFILE_PREFIX + profile + ".properties"));
        }

        return mergedProperties;
    }


    /**
     * 读取yml(含 profile 专属配置文件,如 application-local.yml)
     * @param moduleDirectory
     * @return
     */
    public static Map<String, Object> readYmlOrYaml(PsiDirectory moduleDirectory) {
        // 创建一个新的 Map 用于存储合并后的结果
        Map<String, Object> mergedYamlData = new HashMap<>();

        // 合并基础配置文件，后面的覆盖前面的
        putAll(mergedYamlData, readYmlFromFile(moduleDirectory, YAML_FILE_NAME));
        putAll(mergedYamlData, readYmlFromFile(moduleDirectory, YML_FILE_NAME));
        putAll(mergedYamlData, readYmlFromFile(moduleDirectory, YAML_BOOTSTRAP_FILE_NAME));
        putAll(mergedYamlData, readYmlFromFile(moduleDirectory, YML_BOOTSTRAP_FILE_NAME));

        // 合并 profile 专属配置文件，覆盖基础配置
        for (String profile : resolveActiveProfiles(moduleDirectory)) {
            putAll(mergedYamlData, readYmlFromFile(moduleDirectory, APPLICATION_PROFILE_PREFIX + profile + ".yml"));
            putAll(mergedYamlData, readYmlFromFile(moduleDirectory, APPLICATION_PROFILE_PREFIX + profile + ".yaml"));
            putAll(mergedYamlData, readYmlFromFile(moduleDirectory, BOOTSTRAP_PROFILE_PREFIX + profile + ".yml"));
            putAll(mergedYamlData, readYmlFromFile(moduleDirectory, BOOTSTRAP_PROFILE_PREFIX + profile + ".yaml"));
        }

        return mergedYamlData;
    }

    /**
     * 解析当前生效的 spring profile 列表
     * 依次从基础 properties / yml 的 spring.profiles.active 中解析；
     * 若使用了 maven 占位符(如 @profile.active@)无法解析，则扫描 application-*.yml/properties 文件兜底
     */
    private static List<String> resolveActiveProfiles(PsiDirectory moduleDirectory) {
        List<String> profiles = new ArrayList<>();

        // 从基础 properties 解析
        Properties baseProperties = new Properties();
        baseProperties.putAll(readPropertiesFromFile(moduleDirectory, PROPERTIES_FILE_NAME));
        baseProperties.putAll(readPropertiesFromFile(moduleDirectory, PROPERTIES_BOOTSTRAP_FILE_NAME));
        collectProfileValue(baseProperties.getProperty(SPRING_PROFILES_ACTIVE), profiles);

        // 从基础 yml 解析(支持逗号分隔或列表写法)
        Map<String, Object> baseYml = new HashMap<>();
        putAll(baseYml, readYmlFromFile(moduleDirectory, YAML_FILE_NAME));
        putAll(baseYml, readYmlFromFile(moduleDirectory, YML_FILE_NAME));
        putAll(baseYml, readYmlFromFile(moduleDirectory, YAML_BOOTSTRAP_FILE_NAME));
        putAll(baseYml, readYmlFromFile(moduleDirectory, YML_BOOTSTRAP_FILE_NAME));
        collectYamlProfileValue(extractNestedValue(baseYml, SPRING_PROFILES_ACTIVE), profiles);

        // 兜底:扫描 profile 专属文件名
        if (CollectionUtil.isEmpty(profiles)) {
            collectProfileFileNames(moduleDirectory, profiles);
        }
        return profiles;
    }

    private static void collectYamlProfileValue(Object activeValue, List<String> profiles) {
        if (activeValue instanceof String) {
            collectProfileValue((String) activeValue, profiles);
        } else if (activeValue instanceof List) {
            for (Object item : (List<?>) activeValue) {
                collectProfileValue(item == null ? null : item.toString(), profiles);
            }
        }
    }

    private static void collectProfileValue(String activeValue, List<String> profiles) {
        if (StringUtil.isBlank(activeValue) || activeValue.contains("@")) {
            // 空值或 maven 占位符(如 @profile.active@)跳过
            return;
        }
        for (String profile : activeValue.split(",")) {
            String trimmed = profile.trim();
            if (StringUtil.isNotBlank(trimmed) && !profiles.contains(trimmed)) {
                profiles.add(trimmed);
            }
        }
    }

    private static void collectProfileFileNames(PsiDirectory moduleDirectory, List<String> profiles) {
        if (moduleDirectory == null || moduleDirectory.getVirtualFile() == null) {
            return;
        }
        collectProfileFileNamesRecursively(moduleDirectory.getVirtualFile(), profiles);
    }

    private static void collectProfileFileNamesRecursively(VirtualFile directory, List<String> profiles) {
        if (!directory.isDirectory()) {
            return;
        }
        for (VirtualFile child : directory.getChildren()) {
            if (child.isDirectory()) {
                collectProfileFileNamesRecursively(child, profiles);
            } else {
                String profile = profileFromFileName(child.getName());
                if (StringUtil.isNotBlank(profile) && !profiles.contains(profile)) {
                    profiles.add(profile);
                }
            }
        }
    }

    /**
     * 从 application-{profile}.yml/yaml/properties 或 bootstrap-{profile}.yml 等文件名中提取 profile
     */
    private static String profileFromFileName(String fileName) {
        if (StringUtil.isBlank(fileName)) {
            return null;
        }
        String lower = fileName.toLowerCase();
        if (lower.startsWith(APPLICATION_PROFILE_PREFIX)) {
            return extractProfileSuffix(fileName, APPLICATION_PROFILE_PREFIX);
        }
        if (lower.startsWith(BOOTSTRAP_PROFILE_PREFIX)) {
            return extractProfileSuffix(fileName, BOOTSTRAP_PROFILE_PREFIX);
        }
        return null;
    }

    private static String extractProfileSuffix(String fileName, String prefix) {
        String suffix = fileName.substring(prefix.length());
        int dot = suffix.lastIndexOf('.');
        if (dot <= 0) {
            return null;
        }
        String ext = suffix.substring(dot + 1).toLowerCase();
        if (!"yml".equals(ext) && !"yaml".equals(ext) && !"properties".equals(ext)) {
            return null;
        }
        return suffix.substring(0, dot);
    }

    /**
     * 从 YAML Map 中提取嵌套 key 对应的值，如 spring.profiles.active
     */
    private static Object extractNestedValue(Map<String, Object> yml, String configKey) {
        String[] keys = configKey.split("\\.");
        Object value = yml;
        for (String key : keys) {
            if (value instanceof Map) {
                value = ((Map<?, ?>) value).get(key);
            } else {
                return null;
            }
        }
        return value;
    }

    private static void putAll(Map<String, Object> target, Map<String, Object> source) {
        if (source != null) {
            target.putAll(source);
        }
    }

    /**
     * 解析出properties中的属性
     * @param moduleDirectory
     * @param fileName
     * @return
     */
    private static Properties readPropertiesFromFile(PsiDirectory moduleDirectory, String fileName) {
        Properties properties = new Properties();
        List<VirtualFile> files = findFilesByName(moduleDirectory, fileName);
        for (VirtualFile file : files) {
            try (InputStream inputStream = file.getInputStream()) {
                properties.load(inputStream);
                break; // 只加载第一个找到的文件
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return properties;
    }

    /**
     * 解析出yml中的属性
     * @param moduleDirectory
     * @param fileName
     * @return
     */
    private static Map<String, Object> readYmlFromFile(PsiDirectory moduleDirectory, String fileName) {
        Yaml yaml = new Yaml();
        List<VirtualFile> files = findFilesByName(moduleDirectory, fileName);
        for (VirtualFile file : files) {
            try (InputStream inputStream = file.getInputStream()) {
                return yaml.load(inputStream);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static List<VirtualFile> findFilesByName(PsiDirectory directory, String fileName) {
        if (directory == null || directory.getVirtualFile() == null) {
            return new ArrayList<>();
        }
        List<VirtualFile> foundFiles = new ArrayList<>();
        findFilesByNameRecursively(directory.getVirtualFile(), fileName, foundFiles);
        return foundFiles;
    }

    private static void findFilesByNameRecursively(VirtualFile directory, String fileName, List<VirtualFile> foundFiles) {
        if (!directory.isDirectory()) {
            return;
        }

        for (VirtualFile child : directory.getChildren()) {
            if (child.isDirectory()) {
                findFilesByNameRecursively(child, fileName, foundFiles);
            } else if (fileName.equals(child.getName())) {
                foundFiles.add(child);
            }
        }
    }

}
