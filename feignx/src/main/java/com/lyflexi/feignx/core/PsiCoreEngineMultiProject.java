package com.lyflexi.feignx.core;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.*;

/**
 * @Author: hmly
 * @Date: 2025/3/14 19:42
 * @Project: feignx-plugin
 * @Version: 1.0.0
 * @Description:
 */
@Deprecated
public class PsiCoreEngineMultiProject {
    /**
     * 获取所有打开的项目列表
     *
     * @return {@link Project[]}
     */
    public static Project[] getOpenProjects() {
        // 获取ProjectManager实例
        ProjectManager projectManager = ProjectManager.getInstance();
        // 获取所有打开的项目列表
        return projectManager.getOpenProjects();
    }

    /**
     * 获取工程中所有的class
     *
     * @param rootPackage
     * @param project
     * @return
     */
    public static List<PsiClass> scanProjectCls(PsiPackage rootPackage, Project project) {
        List<PsiClass> javaFiles = new ArrayList<>();
        //只扫描项目中的业务文件，不包含资源文件、配置文件、静态文件等
        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
        processPackage(rootPackage, projectScope, javaFiles);
        return javaFiles;
    }

    /**
     * 递归方法processPackage
     *
     * @param psiPackage
     * @param searchScope
     * @param classesToCheck
     */
    private static void processPackage(PsiPackage psiPackage, GlobalSearchScope searchScope, List<PsiClass> classesToCheck) {
        for (PsiClass psiClass : psiPackage.getClasses()) {
            classesToCheck.add(psiClass);
        }

        for (PsiPackage subPackage : psiPackage.getSubPackages(searchScope)) {
            processPackage(subPackage, searchScope, classesToCheck);
        }
    }
}
