package com.lyflexi.feignx.provider.tabIcon;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.lyflexi.feignx.user.UserRestControllerSettings;
import com.lyflexi.feignx.utils.AnnotationParserUtils;
import com.lyflexi.feignx.core.PsiCoreEngine;
import com.lyflexi.feignx.utils.BizChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @Author: liuyanoutsee@outlook.com
 * @Date: 2025/4/2 20:34
 * @Project: feignx-plugin
 * @Version: 1.0.0
 * @Description: ApiController文件图标
 */
public class RestControllerIconProvider extends IconProvider {
    /**
     * ApiController文件图标
     */
    Icon STATEMENT_LINE_CONTROLLER_ICON = IconLoader.getIcon("/icons/jumpAction_controller.svg");

    @Override
    public @Nullable Icon getIcon(@NotNull PsiElement element, int flags) {
        if (null==element) {
            return null;
        }
        //排除三方依赖扫描
        if (!BizChecker.isBizElement(element)){
            return null;
        }
        if (!(element instanceof PsiClass)) {
            return null;
        }


        PsiClass psiClass = (PsiClass) element;
        // 不对接口类做处理
        if (psiClass.isInterface()) {
            return null;
        }

        //开启ApiController文件图标
        if (!UserRestControllerSettings.getInstance().isIconEnabled()) {
            return null;
        }

        if (AnnotationParserUtils.isControllerClass(psiClass)) {
            return STATEMENT_LINE_CONTROLLER_ICON;
        }

        return null;
    }
}
