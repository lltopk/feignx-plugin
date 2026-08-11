package com.lyflexi.feignx.properties;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;

import java.util.Optional;

/**
 * @Description:
 * @Author: lyflexi
 * @project: feignx-plugin
 * @Date: 2024/11/3 15:01 
 */
public class ServerParser {

    public static Optional<PsiDirectory> getServiceModuleResourcesDirectory(PsiClass psiClass, Project project) {
        // 获取模块的根目录
        PsiDirectory moduleRootDirectory = getModuleRootDirectory(psiClass, project);
        if (moduleRootDirectory == null) {
            return Optional.empty();
        }

        // 在模块根目录下查找 resources 目录
        return Optional.ofNullable(findResourcesDirectory(moduleRootDirectory));
    }

    /**
     * 获取 psiClass 所属模块的根目录(包含 src 的目录)
     */
    public static PsiDirectory getModuleRootDirectory(PsiClass psiClass, Project project) {
        PsiDirectory currentDirectory = psiClass.getContainingFile().getContainingDirectory();

        // 向上查找，直到找到包含 src 目录的模块根目录，或达到项目根目录
        while (currentDirectory != null) {
            VirtualFile parentDir = currentDirectory.getVirtualFile().getParent();
            if (parentDir == null || parentDir.equals(project.getBaseDir())) {
                break; // 已经达到项目根目录，停止查找
            }

            VirtualFile srcFolder = parentDir.findChild("src");
            if (srcFolder != null && srcFolder.isDirectory()) {
                return PsiManager.getInstance(project).findDirectory(parentDir);
            }

            currentDirectory = currentDirectory.getParent();
        }
        return null;
    }

    /**
     * 获取 psiClass 所属模块根目录的路径，用于单次扫描内按模块缓存配置解析结果
     */
    public static String getModuleRootPath(PsiClass psiClass, Project project) {
        PsiDirectory moduleRootDirectory = getModuleRootDirectory(psiClass, project);
        return moduleRootDirectory == null ? "" : moduleRootDirectory.getVirtualFile().getPath();
    }

    private static PsiDirectory findResourcesDirectory(PsiDirectory directory) {
        if (directory == null) {
            return null;
        }

        VirtualFile[] children = directory.getVirtualFile().getChildren();

        for (VirtualFile child : children) {
            if (child.isDirectory()) {
                // 如果找到 resources 目录，返回对应的 PsiDirectory
                if ("resources".equals(child.getName())) {
                    return PsiManager.getInstance(directory.getProject()).findDirectory(child);
                }
                // 查找子目录，并在递归调用前进行 null 检查
                PsiDirectory childDirectory = PsiManager.getInstance(directory.getProject()).findDirectory(child);
                if (childDirectory != null) {
                    PsiDirectory result = findResourcesDirectory(childDirectory);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }


}
