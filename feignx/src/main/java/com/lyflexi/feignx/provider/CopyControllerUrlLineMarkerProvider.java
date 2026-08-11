package com.lyflexi.feignx.provider;

import com.intellij.codeInsight.daemon.*;
import com.intellij.notification.NotificationGroupManager;

import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.lyflexi.feignx.constant.RestIcons;
import com.lyflexi.feignx.entity.HttpMappingInfo;
import com.lyflexi.feignx.utils.AnnotationParserUtils;
import com.lyflexi.feignx.utils.ControllerClassScanUtils;
import com.lyflexi.feignx.utils.ProjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;
import java.util.*;

import com.intellij.notification.NotificationType;
import org.jetbrains.annotations.Nullable;

/**
 * @Author: hmly
 * @Date: 2025/3/9 17:42
 * @Project: feignx-plugin
 * @Version: 1.0.0
 * @Description: 将拷贝功能的Gutter挂在注解上
 */

/*
 * 复制操作不属于RelatedItemLineMarkerProvider，IntelliJ 平台在新版 SDK 中推荐实现LineMarkerProviderDescriptor
 *
 * 1. 实现接口：LineMarkerProviderDescriptor
 * 2. 自定义构造LineMarkerInfo<PsiElement>：由于点击图标时执行复制操作不属于跳转导航操作NavigationGutterIconBuilder，而是需要手动构建 LineMarkerInfo
 *
 *
 * */
public class CopyControllerUrlLineMarkerProvider extends LineMarkerProviderDescriptor {

    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        if (null==element) {
            return null;
        }
        //排除三方依赖扫描
        if (!ProjectUtils.isBizElement(element)){
            return null;
        }
        Project project = element.getProject();
        if (DumbService.isDumb(project)) {
            return null; // 索引未完成，跳过
        }
        if (!(element instanceof PsiMethod)) {
            return null;
        }

        PsiMethod method = (PsiMethod) element;

        // 增加有效性校验
        if (null == method || !method.isValid()) {
            return null;
        }
        if (!AnnotationParserUtils.isElementWithinController(method)) {
            return null;
        }

        //解析restfull注解，下面gutter挂在注解旁
        PsiAnnotation restfulAnnotation = AnnotationParserUtils.findRestfulAnnotation(method);
        if (Objects.isNull(restfulAnnotation)) {
            return null;
        }

        //直接基于当前controller方法计算HttpMappingInfo，不再依赖双边缓存
        HttpMappingInfo controllerCache = ControllerClassScanUtils.controllerOfPsiMethod(method.getContainingClass(), method.getProject(), method);

        if (Objects.isNull(controllerCache)) {
            return null;
        }

        String url = controllerCache.getPath();
        if (StringUtils.isBlank(url)) {
            return null;
        }


        PsiMethod finalMethod = method;
        GutterIconNavigationHandler<PsiElement> handler = (mouseEvent, elt) -> {
            CopyPasteManager.getInstance().setContents(new StringSelection(url));
            NotificationGroupManager.getInstance()
                    .getNotificationGroup("FeignClient Assistant")
                    .createNotification("URL Copied To Clipboard:\n" + url, NotificationType.INFORMATION)
                    .notify(finalMethod.getProject());
        };

        // 构建图标信息，挂在方法上
        LineMarkerInfo<PsiElement> marker = new LineMarkerInfo<>(
                restfulAnnotation,
                restfulAnnotation.getTextRange(),
                RestIcons.STATEMENT_LINE_CLIPBOARD_CONTROLLER_ICON,
                psi -> "Click To Copy Controller-URL: " + url,
                handler,
                GutterIconRenderer.Alignment.RIGHT,
                () -> "Copy Controller URL"
        );

        return marker;
    }


    @Override
    public @Nullable("null means disabled") @GutterName String getName() {
        return "Copy Controller URL";
    }
}
