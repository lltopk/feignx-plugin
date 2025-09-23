package com.lyflexi.feignx.properties;


import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;


import java.util.HashMap;
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


    /**
     * 读取properties
     * @param moduleDirectory
     * @return
     */
    public static Properties readProperties(PsiDirectory moduleDirectory) {
        Properties properties1 = readPropertiesFromFile(moduleDirectory, PROPERTIES_FILE_NAME);
        Properties properties2 = readPropertiesFromFile(moduleDirectory, PROPERTIES_BOOTSTRAP_FILE_NAME);

        // 创建一个新的 Properties 对象用于存储整合后的结果
        Properties mergedProperties = new Properties();

        // 将 properties1 和 properties2 合并到 mergedProperties
        // 合并数据，后面的覆盖前面的
        if (properties1 != null&&!properties1.isEmpty()) {
            mergedProperties.putAll(properties1);
        }
        if (properties2 != null&&!properties2.isEmpty()) {
            mergedProperties.putAll(properties2);
        }

        return mergedProperties;
    }


    /**
     * 读取yml
     * @param moduleDirectory
     * @return
     */
    public static Map<String, Object> readYmlOrYaml(PsiDirectory moduleDirectory) {
        // 读取每个 YAML 文件的内容到对应的 Map 中

        Map<String, Object> yamlData1 = readYmlFromFile(moduleDirectory, YAML_FILE_NAME);
        Map<String, Object> yamlData2 = readYmlFromFile(moduleDirectory, YML_FILE_NAME);
        Map<String, Object> yamlData3 = readYmlFromFile(moduleDirectory, YAML_BOOTSTRAP_FILE_NAME);
        Map<String, Object> yamlData4 = readYmlFromFile(moduleDirectory, YML_BOOTSTRAP_FILE_NAME); // 注意最后文件变量是否正确

        // 创建一个新的 Map 用于存储合并后的结果
        Map<String, Object> mergedYamlData = new HashMap<>();

        // 合并数据，后面的覆盖前面的
        if (yamlData1 != null && !yamlData1.isEmpty()) {
            mergedYamlData.putAll(yamlData1);
        }
        if (yamlData2 != null  && !yamlData2.isEmpty()) {
            mergedYamlData.putAll(yamlData2);
        }
        if (yamlData3 != null && !yamlData3.isEmpty()) {
            mergedYamlData.putAll(yamlData3);
        }
        if (yamlData4 != null && !yamlData4.isEmpty()) {
            mergedYamlData.putAll(yamlData4);
        }

        return mergedYamlData;
    }

    /**
     * 解析出properties中的属性
     * @param moduleDirectory
     * @param fileName
     * @return
     */
    private static Properties readPropertiesFromFile(PsiDirectory moduleDirectory, String fileName) {
        Properties properties = new Properties();
        VirtualFile[] files = findFilesByName(moduleDirectory, fileName);
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
        VirtualFile[] files = findFilesByName(moduleDirectory, fileName);
        for (VirtualFile file : files) {
            try (InputStream inputStream = file.getInputStream()) {
                return yaml.load(inputStream);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static VirtualFile[] findFilesByName(PsiDirectory directory, String fileName) {
        if (directory == null || directory.getVirtualFile() == null) {
            return new VirtualFile[0];
        }

        return findFilesByNameRecursively(directory.getVirtualFile(), fileName);
    }

    private static VirtualFile[] findFilesByNameRecursively(VirtualFile directory, String fileName) {
        if (!directory.isDirectory()) {
            return new VirtualFile[0];
        }

        VirtualFile[] foundFiles = new VirtualFile[0];

        for (VirtualFile child : directory.getChildren()) {
            if (child.isDirectory()) {
                foundFiles = concatenate(foundFiles, findFilesByNameRecursively(child, fileName));
            } else if (fileName.equals(child.getName())) {
                foundFiles = concatenate(foundFiles, new VirtualFile[]{child});
            }
        }

        return foundFiles;
    }

    private static VirtualFile[] concatenate(VirtualFile[] array1, VirtualFile[] array2) {
        VirtualFile[] result = new VirtualFile[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

}
