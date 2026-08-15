package com.lyflexi.feignx.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;

public class BizChecker {
    /**
     * @description: 检查元素（PsiMethod或者PsiClass）是纯粹的业务文件，而非三方源码，用于过滤所有的Provider监听
     * @author: hmly
     * @date: 2025/5/18 13:50
     * @param: [element]
     * @return: java.lang.Boolean
     **/
    public static Boolean isBizElement(PsiElement element) {
        if (element == null) {
            return false;
        }

        // 检查文件类型
        if (element.getContainingFile() == null) {
            return false;
        }

        //element所属的文件
        VirtualFile virtualFile = element.getContainingFile().getVirtualFile();
        if (virtualFile == null) {
            return false;
        }

        // 首先检查是否是Java文件。
        String fileName = virtualFile.getName();
        if (!fileName.endsWith(".java")) {
            return false;
        }

        // 然后排除三方包中的文件
        Project project = element.getProject();

        ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);

        if (projectFileIndex.isInLibrary(virtualFile)) {
            return false;
        }

        return projectFileIndex.isInSourceContent(virtualFile);
    }
}
