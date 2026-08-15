package com.lyflexi.feignx.provider.tabIcon;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.lyflexi.feignx.user.UserFeignSettings;
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
 * @Description: FeignClient文件图标
 */
public class FeignClassIconProvider extends IconProvider {
    /**
     * FeignClient文件图标
     */
    Icon STATEMENT_LINE_FEIGN_ICON = IconLoader.getIcon("/icons/jumpAction_feign.svg");

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
        // 只对接口类做处理
        if (!psiClass.isInterface()) {
            return null;
        }

        //开启FeignClient文件图标
        if (!UserFeignSettings.getInstance().isIconEnabled()) {
            return null;
        }

        if (AnnotationParserUtils.isFeignInterface(psiClass)) {
            return STATEMENT_LINE_FEIGN_ICON;
        }

        return null;
    }
}
