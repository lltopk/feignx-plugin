package com.lyflexi.feignx.listener.project;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.PsiTreeChangeListener;
import com.lyflexi.feignx.cache.InitialPsiClassCacheManager;
import com.lyflexi.feignx.listener.project.handler.PsiClassAddHandler;
import com.lyflexi.feignx.listener.project.handler.PsiClassChangeHandler;
import com.lyflexi.feignx.listener.project.handler.PsiClassModifyHandler;
import com.lyflexi.feignx.utils.AnnotationParserUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * git pull之后，或者git pull origin other-branch之后
 *
 * 由于会触发psi索引树的变更，一旦存在与feign接口或者controller接口相关的改动，就会导致gutter失效
 */
public class PsiClassGitChangeListener implements PsiTreeChangeListener {

    /**
     * 从PsiTreeChangeEvent中获取关联的Project；获取不到时返回null，调用方安全跳过即可。
     */
    @Nullable
    private static Project getProject(PsiTreeChangeEvent event) {
        PsiElement element = event.getChild();
        if (element == null) {
            element = event.getParent();
        }
        if (element == null) return null;
        PsiFile file = element.getContainingFile();
        if (file == null) return null;
        return file.getProject();
    }

    //aftered事件
    @Override
    public void childAdded(@NotNull PsiTreeChangeEvent event) {
        handleAddEvent(event);
    }

    @Override public void childRemoved(PsiTreeChangeEvent event) {
        
    }

    /**
     * 监听被git替换的类
     * @param event the event object describing the change.
     */
    @Override public void childReplaced(PsiTreeChangeEvent event) {
        handleModifyEvent(event);
    }

    /**
     * 监听被git修改的类
     * @param event the event object describing the change.
     */
    @Override public void childrenChanged(PsiTreeChangeEvent event) {
        handleModifyEvent(event);
    }

    @Override public void propertyChanged(PsiTreeChangeEvent event) {
        handleModifyEvent(event);
    }

    @Override
    public void childMoved(@NotNull PsiTreeChangeEvent event) {
        handleModifyEvent(event);
    }

    //before事件
    @Override public void beforeChildAddition(PsiTreeChangeEvent event) {}
    @Override public void beforeChildRemoval(PsiTreeChangeEvent event) {}
    @Override public void beforeChildrenChange(PsiTreeChangeEvent event) {}

    @Override
    public void beforePropertyChange(@NotNull PsiTreeChangeEvent event) {

    }

    @Override public void beforeChildReplacement(PsiTreeChangeEvent event) {}

    @Override
    public void beforeChildMovement(@NotNull PsiTreeChangeEvent event) {

    }
    /**
     * 处理修改事件变动,先删除再新增
     * @param event
     */
    private void handleModifyEvent(@NotNull PsiTreeChangeEvent event) {
        Project project = getProject(event);
        // 索引未完成，!禁止自旋调用，否则项目启动时候会OOM
        if (project == null || DumbService.isDumb(project)) {
            return;
        }
        PsiElement child = event.getChild();
        if (!(child instanceof PsiClass)) {
            return;
        }
        PsiClass psiClass = (PsiClass) child;
        //只监听controller和feignclient，提升监听处理性能
        if (AnnotationParserUtils.isControllerClass(psiClass) || AnnotationParserUtils.isFeignInterface(psiClass)){
            // 监听到变动的类了，先删除类缓存再新增类缓存
            System.out.println("监听到psi引擎变动的psiclass类: {}" + psiClass.getQualifiedName());
            // 这里调用的 ProjectUtils 中缓存添加逻辑
            PsiClassChangeHandler modifyHandler = new PsiClassModifyHandler();
            modifyHandler.handle(project, psiClass);
        }
    }

    /**
     * 处理新增事件变动
     * @param event
     */
    private void handleAddEvent(@NotNull PsiTreeChangeEvent event) {
        Project project = getProject(event);
        // 索引未完成，!禁止自旋调用，否则项目启动时候会OOM
        if (project == null || DumbService.isDumb(project)) {
            return;
        }
        PsiElement child = event.getChild();
        if (!(child instanceof PsiClass)) {
            return;
        }
        PsiClass psiClass = (PsiClass) child;
        //只监听controller和feignclient，提升监听处理性能
        if (AnnotationParserUtils.isControllerClass(psiClass) || AnnotationParserUtils.isFeignInterface(psiClass)){
            // 监听到新增类了，添加到缓存中或触发重新扫描
            System.out.println("监听到psi引擎变动的psiclass类: {}" + psiClass.getQualifiedName());
            // 这里调用的 ProjectUtils 中缓存添加逻辑
            PsiClassChangeHandler addHandler = new PsiClassAddHandler();
            addHandler.handle(project, psiClass);
        }
    }
}
