package com.lyflexi.feignx.statusbar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import com.lyflexi.feignx.constant.RestIcons;
import com.lyflexi.feignx.refresh.FeignRefreshManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;

/**
 * 右下角 StatusBar 图标:点击手动刷新所有 Feign/Controller 映射(Issue #25 兜底功能)
 * <p>
 * 仅承载 UI 图标与点击事件转发,刷新逻辑见 {@link FeignRefreshManager},
 * 与扫描/匹配逻辑完全解耦。
 */
public class FeignRefreshStatusBarWidget implements StatusBarWidget, StatusBarWidget.IconPresentation, StatusBarWidget.Multiframe {

    private final Project project;

    public FeignRefreshStatusBarWidget(Project project) {
        this.project = project;
    }

    @Override
    public @NotNull String ID() {
        return FeignRefreshStatusBarWidgetFactory.WIDGET_ID;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public WidgetPresentation getPresentation() {
        return this;
    }

    @Override
    public @Nullable Icon getIcon() {
        // 使用插件图标,并按 StatusBar 紧凑尺寸自适应缩放,避免显示过大
        return RestIcons.STATUS_BAR_ICON;
    }

    @Override
    public @Nullable String getTooltipText() {
        return "Refresh all FeignClients and Controllers";
    }

    @Override
    public @Nullable Consumer<MouseEvent> getClickConsumer() {
        return mouseEvent -> FeignRefreshManager.refreshAll(project);
    }

    @Override
    public StatusBarWidget copy() {
        return new FeignRefreshStatusBarWidget(project);
    }
}
