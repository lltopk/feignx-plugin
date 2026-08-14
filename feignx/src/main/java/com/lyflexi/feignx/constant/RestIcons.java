package com.lyflexi.feignx.constant;

import com.intellij.openapi.util.IconLoader;
import com.intellij.util.IconUtil;

import javax.swing.*;

/**
 * @Description:
 * @Author: lyflexi
 * @project: feignx-plugin
 * @Date: 2024/10/18 14:56
 */
public interface RestIcons {
    Icon STATEMENT_LINE_FEIGN_ICON = IconLoader.getIcon("/icons/jumpAction_feign.svg", RestIcons.class);
    Icon STATEMENT_LINE_CONTROLLER_ICON = IconLoader.getIcon("/icons/jumpAction_controller.svg", RestIcons.class);
    Icon STATEMENT_LINE_OPEN_API_ICON = IconLoader.getIcon("/icons/openApi.svg", RestIcons.class);

    // 插件图标(16x16)。
    // 注意:不使用 /META-INF/pluginIcon.svg(插件类加载器下该路径图标加载异常会显示默认占位图标),
    // 而是引用 /icons/ 下与 META-INF/pluginIcon.svg 同内容的副本(与 gutter 图标同目录,已验证可正常加载)
    Icon PLUGIN_ICON = IconLoader.getIcon("/icons/pluginIcon.svg", RestIcons.class);

    // StatusBar 展示的紧凑尺寸(逻辑像素)
    int STATUS_BAR_ICON_SIZE = 13;

    // StatusBar 专用:按源图标实际尺寸自适应缩小到紧凑尺寸,避免右下角图标过大(Issue #25)
    Icon STATUS_BAR_ICON = IconUtil.scale(PLUGIN_ICON, (double) STATUS_BAR_ICON_SIZE / PLUGIN_ICON.getIconWidth());
}

