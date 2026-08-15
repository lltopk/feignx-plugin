package com.lyflexi.feignx.statusbar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBarWidgetFactory;

import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.lyflexi.feignx.core.ProjectRefreshManager;
import org.jetbrains.annotations.NotNull;

/**
 * StatusBar 右下角"手动刷新"图标工厂(Issue #25)
 * <p>
 * 通过 plugin.xml 的 statusBarWidgetFactory 扩展点注册。
 * 仅负责创建/销毁 UI 组件,刷新逻辑委托给 {@link ProjectRefreshManager}。
 */
public class FeignRefreshStatusBarWidgetFactory implements StatusBarWidgetFactory {

    /**
     * 组件唯一标识,用于持久化显隐配置,同时作为 {@link FeignRefreshStatusBarWidget#ID()}
     */
    public static final String WIDGET_ID = "com.lyflexi.feignx.refreshWidget";

    private static final String DISPLAY_NAME = "Feign/Controller 手动刷新";

    @Override
    public @NotNull String getId() {
        return WIDGET_ID;
    }

    @Override
    public @NotNull String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return true;
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new FeignRefreshStatusBarWidget(project);
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
    }

    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        return true;
    }
}
