package com.lyflexi.feignx.properties;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @Description:
 * @Author: lyflexi
 * @project: feignx-plugin
 * @Date: 2024/11/3 15:01
 */
public class ServerParser {

    /**
     * resources 目录的标准位置：<moduleRoot>/src/main/resources
     */
    private static final String SRC_DIR_NAME = "src";
    private static final String MAIN_DIR_NAME = "main";
    private static final String RESOURCES_DIR_NAME = "resources";

    /**
     * 兜底递归查找时的最大深度，避免在异常布局下无限下探
     */
    private static final int MAX_FALLBACK_DEPTH = 4;

    /**
     * 兜底递归查找时跳过的目录：构建产物与版本库目录噪声大且可能正在被并发写入，
     * 遍历它们既拖慢扫描，又极易触发 VFS 的 FileDeletedException
     */
    private static final Set<String> SKIPPED_DIR_NAMES = new HashSet<>(Arrays.asList(
            "target", "build", "out", "bin", "node_modules",
            ".git", ".idea", ".svn", ".hg", "gradle", "logs"
    ));

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

            VirtualFile srcFolder = findChildSafely(parentDir, SRC_DIR_NAME);
            if (srcFolder != null) {
                return PsiManager.getInstance(project).findDirectory(srcFolder);
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

    /**
     * 在模块根目录下定位 resources 目录。
     * <p>
     * 优先按标准 Maven/Gradle 布局定点查找 {@code src/main/resources}，
     * 该路径是常量级的定向 VFS 查询，不会遍历目录树；
     * 仅在标准布局不存在时才启用兜底递归。
     * <p>
     * 背景：此前实现无条件递归遍历整个模块目录树查找名为 resources 的目录，
     * 既会在大工程上产生显著开销，又会跨越 target/.git 等正在被并发读写的目录，
     * 从而抛出 {@code FileDeletedException} 中断整次扫描。
     */
    private static PsiDirectory findResourcesDirectory(PsiDirectory moduleRootDirectory) {
        if (moduleRootDirectory == null) {
            return null;
        }

        // 标准布局：src/main/resources，全程使用 findChild 定向查询，不展开目录树
        PsiDirectory resourcesDirectory = findStandardResourcesDirectory(moduleRootDirectory);
        if (resourcesDirectory != null) {
            return resourcesDirectory;
        }

        // 非标准布局兜底
        return findResourcesDirectoryFallback(moduleRootDirectory, 0, new HashSet<>());
    }

    /**
     * 按标准布局 src/main/resources 定点查找，全程不调用 getChildren()
     */
    private static PsiDirectory findStandardResourcesDirectory(PsiDirectory moduleRootDirectory) {
        PsiDirectory srcDirectory = findSubdirectory(moduleRootDirectory, SRC_DIR_NAME);
        if (srcDirectory == null) {
            return null;
        }
        PsiDirectory mainDirectory = findSubdirectory(srcDirectory, MAIN_DIR_NAME);
        if (mainDirectory == null) {
            return null;
        }
        return findSubdirectory(mainDirectory, RESOURCES_DIR_NAME);
    }

    /**
     * 非标准布局的兜底递归查找。
     * <p>
     * 限定最大深度、跳过构建产物目录、并用 visited 集合防止软链接造成的环路；
     * 同时对每一层做异常兜底，单个目录在扫描过程中被删除不应中断整次扫描。
     */
    private static PsiDirectory findResourcesDirectoryFallback(PsiDirectory directory, int depth, Set<String> visited) {
        if (directory == null || depth > MAX_FALLBACK_DEPTH) {
            return null;
        }

        VirtualFile directoryFile = directory.getVirtualFile();
        if (directoryFile == null || !directoryFile.isValid()) {
            return null;
        }
        // 以路径去重，避免软链接/目录硬链接导致的环路无限递归
        if (!visited.add(directoryFile.getPath())) {
            return null;
        }

        VirtualFile[] children;
        try {
            children = directoryFile.getChildren();
        } catch (RuntimeException e) {
            // 目录在遍历过程中被删除或刷新，FileDeletedException 等均属于可恢复情况
            return null;
        }
        if (children == null) {
            return null;
        }

        for (VirtualFile child : children) {
            // 扫描期间文件可能随时被删除，失效节点必须跳过
            if (child == null || !child.isValid() || !child.isDirectory()) {
                continue;
            }
            if (SKIPPED_DIR_NAMES.contains(child.getName())) {
                continue;
            }
            // 当前层就是 resources，直接返回
            if (RESOURCES_DIR_NAME.equals(child.getName())) {
                PsiDirectory childDirectory = findSubdirectoryByFile(directory, child);
                if (childDirectory != null) {
                    return childDirectory;
                }
                continue;
            }

            PsiDirectory childDirectory = findSubdirectoryByFile(directory, child);
            if (childDirectory != null) {
                PsiDirectory result = findResourcesDirectoryFallback(childDirectory, depth + 1, visited);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * 安全地按名称查找子目录，全程异常兜底
     */
    private static PsiDirectory findSubdirectory(PsiDirectory parent, String name) {
        if (parent == null || name == null) {
            return null;
        }
        VirtualFile child = findChildSafely(parent.getVirtualFile(), name);
        if (child == null) {
            return null;
        }
        return findSubdirectoryByFile(parent, child);
    }

    private static PsiDirectory findSubdirectoryByFile(PsiDirectory parent, VirtualFile childFile) {
        try {
            PsiDirectory psiDirectory = PsiManager.getInstance(parent.getProject()).findDirectory(childFile);
            return psiDirectory != null && psiDirectory.isValid() ? psiDirectory : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 安全地查找直接子节点：过滤失效目录，并对 VFS 竞态异常做兜底
     */
    private static VirtualFile findChildSafely(VirtualFile parent, String name) {
        if (parent == null || !parent.isValid()) {
            return null;
        }
        try {
            VirtualFile child = parent.findChild(name);
            if (child == null || !child.isValid() || !child.isDirectory()) {
                return null;
            }
            return child;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
