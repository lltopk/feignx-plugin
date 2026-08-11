package com.lyflexi.feignx.utils;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.lyflexi.feignx.enums.SpringCloudClassAnnotation;
import com.lyflexi.feignx.enums.SpringBootClassAnnotation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: hmly
 * @Date: 2025/3/14 19:42
 * @Project: feignx-plugin
 * @Version: 1.0.0
 * @Description:
 */
public class ProjectUtils {
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

    /**
     * 基于 IntelliJ 注解索引(AnnotatedElementsSearch)精确扫描所有 Controller 类(@Controller/@RestController)
     * 相比全包递归,只命中索引中标注了目标注解的类,复杂度从 O(全项目类) 降为 O(标注类)
     *
     * @param project
     * @return
     */
    public static List<PsiClass> scanAllControllerClasses(Project project) {
        return searchClassesByAnnotation(project,
                SpringBootClassAnnotation.CONTROLLER.getQualifiedName(),
                SpringBootClassAnnotation.RESTCONTROLLER.getQualifiedName());
    }

    /**
     * 基于 IntelliJ 注解索引(AnnotatedElementsSearch)精确扫描所有 Feign 接口(@FeignClient)
     *
     * @param project
     * @return
     */
    public static List<PsiClass> scanAllFeignClasses(Project project) {
        return searchClassesByAnnotation(project, SpringCloudClassAnnotation.FEIGNCLIENT.getQualifiedName());
    }

    /**
     * 注解索引扫描通用实现
     * 注意:查找注解类本身必须用 allScope(注解类在三方依赖库中),而检索被标注的业务类用 projectScope
     */
    private static List<PsiClass> searchClassesByAnnotation(Project project, String... annotationQualifiedNames) {
        if (DumbService.isDumb(project)) {
            return Collections.emptyList();
        }
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        GlobalSearchScope allScope = GlobalSearchScope.allScope(project);
        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
        Set<PsiClass> result = new LinkedHashSet<>();
        for (String annotationQualifiedName : annotationQualifiedNames) {
            PsiClass annotationClass = facade.findClass(annotationQualifiedName, allScope);
            if (Objects.isNull(annotationClass)) {
                continue;
            }
            result.addAll(AnnotatedElementsSearch.searchPsiClasses(annotationClass, projectScope).findAll());
        }
        return new ArrayList<>(result);
    }


    /**
     * 扫描所有controller类,使用 IntelliJ 的 类快速索引缓存系统PsiShortNamesCache
     * 方式	                            推荐场景	                                                           性能
     * 手写全类递归	                    小项目或通用工具	                                                   ❌慢(最笨的)
     * AnnotatedElementsSearch         专注指定注解类比如Controller和RestController	                           🚀快(基于直接查询所有类并检查注解的方式。如果项目非常大，这可能会比较耗时)
     * PsiShortNamesCache	          ✅最推荐，快速按类名索引	                                               🚀🚀快
     * 多模块并发	                        大型项目	                                                           🚀🚀🚀
     *
     * @param project
     * @param scope
     * @return 注意需要自己再判断目标psiClass类型：是否是Controller类
     *
     * TODO 但是目前返回为空？原因未知
     */
    @Deprecated
    public static List<PsiClass> scanAllControllerClassesByPsiShortNamesCache(Project project, GlobalSearchScope scope) {
        List<PsiClass> controllerClasses = new ArrayList<>();
        PsiShortNamesCache shortNamesCache = PsiShortNamesCache.getInstance(project);
        DumbService.getInstance(project).runReadActionInSmartMode(() -> {
            //这里总是会报异常，      catch (ProcessCanceledException | IndexNotReadyException e) {
            //        throw e;
            //      }
            // 触发场景：项目刚启动、索引还没完成，或索引被重建时调用 PSI 索引相关 API（比如 getAllClassNames()）就会抛这个。
            //解决方案，包装到runReadActionInSmartMode模式内使用
            String[] allClassNames = shortNamesCache.getAllClassNames();
            for (String className : allClassNames) {
                PsiClass[] classes = shortNamesCache.getClassesByName(className, scope);
                for (PsiClass psiClass : classes) {
                    if (AnnotationParserUtils.isControllerClass(psiClass)) {
                        controllerClasses.add(psiClass);
                    }
                }
            }
        });
        return controllerClasses;
    }

    /**
     * 扫描所有feign类,使用 IntelliJ 的 类快速索引缓存系统PsiShortNamesCache
     * 方式	                            推荐场景	                         性能
     * 手写全类递归	                    小项目或通用工具	                  ❌慢（最笨的）
     * AnnotatedElementsSearch         专注注解类,比如FeignClient	          🚀快(基于直接查询所有类并检查注解的方式。如果项目非常大，这可能会比较耗时)
     * PsiShortNamesCache	          ✅最推荐，快速按类名索引	              🚀🚀快
     * 多模块并发	                        大型项目	                          🚀🚀🚀
     *
     * @param project
     * @param scope
     * @return 注意需要自己再判断目标psiClass类型：是否是Feign类
     *
     * TODO 但是目前返回为空？原因未知
     */
    @Deprecated
    public static List<PsiClass> scanAllFeignClassesByPsiShortNamesCache(Project project, GlobalSearchScope scope) {
        List<PsiClass> feignClass = new ArrayList<>();
        PsiShortNamesCache shortNamesCache = PsiShortNamesCache.getInstance(project);
        DumbService.getInstance(project).runReadActionInSmartMode(() -> {
            //这里总是会报异常，      catch (ProcessCanceledException | IndexNotReadyException e) {
            //        throw e;
            //      }
            // 触发场景：项目刚启动、索引还没完成，或索引被重建时调用 PSI 索引相关 API（比如 getAllClassNames()）就会抛这个。
            //解决方案，包装到runReadActionInSmartMode模式内使用
            String[] allClassNames = shortNamesCache.getAllClassNames();

            for (String className : allClassNames) {
                PsiClass[] classes = shortNamesCache.getClassesByName(className, scope);
                for (PsiClass psiClass : classes) {
                    if (AnnotationParserUtils.isFeignInterface(psiClass)) {
                        feignClass.add(psiClass);
                    }
                }
            }
        });
        return feignClass;
    }


    /**
     * 使用 AnnotatedElementsSearch 精确扫描所有 Controller 类（包含 @Controller 和 @RestController）
     *
     * @param project
     * @param scope
     * @return
     *
     * TODO 但是目前返回为空？原因未知
     */
    @Deprecated
    public static List<PsiClass> scanAllControllerClassesByAnnotationSearch(Project project, GlobalSearchScope scope) {
        List<PsiClass> controllerClasses = new ArrayList<>();
        if (DumbService.isDumb(project)) {
            return Collections.emptyList(); // 索引未完成，跳过
        }

        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);

        PsiClass controllerAnnotation = facade.findClass(SpringBootClassAnnotation.CONTROLLER.getQualifiedName(), scope);
        PsiClass restControllerAnnotation = facade.findClass(SpringBootClassAnnotation.RESTCONTROLLER.getQualifiedName(), scope);

        if (Objects.nonNull(controllerAnnotation)) {
            Collection<PsiClass> classes = AnnotatedElementsSearch.searchPsiClasses(controllerAnnotation, scope).findAll();
            controllerClasses.addAll(classes);
        }

        if (Objects.nonNull(restControllerAnnotation)) {
            Collection<PsiClass> classes = AnnotatedElementsSearch.searchPsiClasses(restControllerAnnotation, scope).findAll();
            controllerClasses.addAll(classes);
        }

        // 可选：去重（如果某个类同时标注了两个注解）
        controllerClasses = controllerClasses.stream()
                .distinct()
                .collect(Collectors.toList());

        return controllerClasses;
    }

    /**
     * 使用 AnnotatedElementsSearch 精确扫描所有 Feign 类（包含 @FeignClient）
     * @param project
     * @param scope
     * @return
     *
     * TODO 但是目前返回为空？原因未知
     */
    @Deprecated
    public static List<PsiClass> scanAllFeignClassesByAnnotationSearch(Project project, GlobalSearchScope scope) {
        List<PsiClass> feignClasss = new ArrayList<>();
        if (DumbService.isDumb(project)) {
            return Collections.emptyList(); // 索引未完成，跳过
        }

        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);

        PsiClass controllerAnnotation = facade.findClass(SpringCloudClassAnnotation.FEIGNCLIENT.getQualifiedName(), scope);

        if (Objects.nonNull(controllerAnnotation)) {
            Collection<PsiClass> classes = AnnotatedElementsSearch.searchPsiClasses(controllerAnnotation, scope).findAll();
            feignClasss.addAll(classes);
        }

        return feignClasss;
    }


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
