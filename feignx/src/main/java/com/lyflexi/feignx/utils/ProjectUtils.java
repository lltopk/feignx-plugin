package com.lyflexi.feignx.utils;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.java.stubs.index.JavaAnnotationIndex;
import com.intellij.psi.search.FileTypeIndex;
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
    public static List<PsiClass> searchClassesByAnnotation(Project project, String... annotationQualifiedNames) {
        if (DumbService.isDumb(project)) {
            return Collections.emptyList();
        }
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        GlobalSearchScope allScope = GlobalSearchScope.allScope(project);
        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
        Set<PsiClass> result = new LinkedHashSet<>();
        for (String annotationQualifiedName : annotationQualifiedNames) {
            //allScope用来寻找jar包中的原始注解， 原始注解是总依据， 后用于匹配项目中的引用注解
            PsiClass annotationClass = facade.findClass(annotationQualifiedName, allScope);
            if (Objects.isNull(annotationClass)) {
                continue;
            }
            //下面不用allScope， 用projectScope即可
            try {
                collectJavaClassesByAnnotation(annotationClass, project, projectScope, result);
            } catch (ProcessCanceledException | IndexNotReadyException e) {
                // 取消/索引未就绪需原样抛出,由上层进度框架处理,不能被兜底逻辑吞掉
                throw e;
            } catch (RuntimeException | LinkageError e) {
                // 兜底:极少数异常场景下回退到仅遍历 Java 源文件匹配注解
                result.addAll(searchJavaClassesByAnnotation(annotationClass, projectScope));
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * 主路径:基于 Java 注解 stub 索引(JavaAnnotationIndex)按短名查注解,
     * 再用 ref.resolve() + areElementsEquivalent 精确匹配目标注解类, 向上取到标注的类。
     * 用:注解 stub 索引 + PSI 引用解析,索引级 O(标注类) 定位,快。
     * 避:AnnotatedElementsSearch 扩展点——会触发 Kotlin 插件的 KotlinAnnotatedElementsSearcher,
     *    Analysis API 未就绪时抛 Cannot find service KaGlobalSearchScopeMerger 等异常,中断整条查询。
     */
    private static void collectJavaClassesByAnnotation(PsiClass annotationClass, Project project,
                                                       GlobalSearchScope scope, Set<PsiClass> result) {
        String shortName = annotationClass.getName();
        if (shortName == null) {
            return;
        }
        //三层关系：
        // owner: PsiClass (FooController)
        // PsiModifierList: 当前的annotation.getContext()
        // annotation: 当前的annotation从 JavaAnnotationIndex 查到
        PsiManager psiManager = PsiManager.getInstance(project);
        Collection<PsiAnnotation> annotations = JavaAnnotationIndex.getInstance().get(shortName, project, scope);
        for (PsiAnnotation annotation : annotations) {
            // 类级注解的父节点是 PsiModifierList,其父节点才是被标注的类
            // (注意:PsiAnnotation.getOwner() 对类级声明注解会返回 null,不能使用)
            PsiElement parent = annotation.getContext();
            if (!(parent instanceof PsiModifierList)) {
                continue;
            }
            PsiElement owner = parent.getParent();
            if (!(owner instanceof PsiClass)) {
                continue;
            }
            // 精确匹配:解析注解引用,确认就是目标注解类(而非同名注解)
            PsiJavaCodeReferenceElement ref = annotation.getNameReferenceElement();
            if (ref != null) {
                PsiElement resolved = ref.resolve();
                if (resolved == null || !psiManager.areElementsEquivalent(resolved, annotationClass)) {
                    continue;
                }
            }
            result.add((PsiClass) owner);
        }
    }

    /**
     * 兜底路径:不走任何注解/搜索索引,按文件类型索引(FileTypeIndex)全量枚举工程内所有 .java 文件,
     * 再逐个用 hasAnnotation(全限定名) 字符串匹配(含内部类)。
     * 用:FileTypeIndex + 字符串匹配,不触碰注解 stub 索引/PSI 树导航,失败面最小。
     * 避:两类索引检索——注解 stub 索引(JavaAnnotationIndex)与 *Search 扩展点(AnnotatedElementsSearch,会触发 KotlinAnnotatedElementsSearcher);
     *
     * 仅在其抛 RuntimeException|LinkageError 时兜底(O(全部文件),慢)。
     */
    private static List<PsiClass> searchJavaClassesByAnnotation(PsiClass annotationClass, GlobalSearchScope scope) {
        List<PsiClass> result = new ArrayList<>();
        String annotationQualifiedName = annotationClass.getQualifiedName();
        if (annotationQualifiedName == null) {
            return result;
        }
        PsiManager psiManager = PsiManager.getInstance(annotationClass.getProject());
        Collection<VirtualFile> javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, scope);
        for (VirtualFile file : javaFiles) {
            PsiFile psiFile = psiManager.findFile(file);
            if (!(psiFile instanceof PsiJavaFile)) {
                continue;
            }
            collectAnnotatedClasses(((PsiJavaFile) psiFile).getClasses(), annotationQualifiedName, result);
        }
        return result;
    }

    /**
     * 递归收集带指定注解的类(含内部类)
     */
    private static void collectAnnotatedClasses(PsiClass[] classes, String annotationQualifiedName, List<PsiClass> result) {
        for (PsiClass psiClass : classes) {
            if (psiClass.hasAnnotation(annotationQualifiedName)) {
                result.add(psiClass);
            }
            collectAnnotatedClasses(psiClass.getInnerClasses(), annotationQualifiedName, result);
        }
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
