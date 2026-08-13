package com.lyflexi.feignx.toolwindow;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * "feignx assistant endpoints" 右侧侧边栏工厂(独立功能,不与历史 gutter/状态栏代码耦合)。
 * <p>
 * 通过 plugin.xml 的 toolWindow 扩展点注册,锚定在右侧,图标复用 icons/jumpAction_controller.svg。
 *
 * @Author: lyflexi
 * @project: feignx-plugin
 */
public class EndpointsToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        EndpointsPanel panel = new EndpointsPanel(project);
        Content content = ContentFactory.SERVICE.getInstance().createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
