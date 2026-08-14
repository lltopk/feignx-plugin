package com.lyflexi.feignx.user;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @Author: liuyanoutsee@outlook.com
 * @Date: 2025/4/2 21:21
 * @Project: feignx-plugin
 * @Version: 1.0.0
 * @Description: 这是个idea设置页面。自定义控制controller文件图标和FeignClient文件图标的开启
 */
public class UserPluginConfigurableUI implements Configurable {

    private JCheckBox feignIconEnabledCheckBox;
    private JCheckBox controllerIconEnabledCheckBox;
    private JPanel mainPanel;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "FeignClient Assistant With RequestX";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mainPanel = new JPanel();
        feignIconEnabledCheckBox = new JCheckBox("Enable FeignClient Tab Icons", UserFeignSettings.getInstance().isIconEnabled());
        controllerIconEnabledCheckBox = new JCheckBox("Enable ApiController Tab Icons", UserRestControllerSettings.getInstance().isIconEnabled());
        mainPanel.add(feignIconEnabledCheckBox);
        mainPanel.add(controllerIconEnabledCheckBox);
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        boolean b1 = feignIconEnabledCheckBox.isSelected() != UserFeignSettings.getInstance().isIconEnabled();
        boolean b2 = controllerIconEnabledCheckBox.isSelected() != UserRestControllerSettings.getInstance().isIconEnabled();
        return b1 || b2;
    }

    @Override
    public void apply() {
        UserFeignSettings.getInstance().setIconEnabled(feignIconEnabledCheckBox.isSelected());
        UserRestControllerSettings.getInstance().setIconEnabled(controllerIconEnabledCheckBox.isSelected());
        // 需要手动刷新图标吗？好像不需要，但有些IDEA版本需要，所以最好提示用户重启项目生效
        showRestartDialog();
    }

    @Override
    public void reset() {
        feignIconEnabledCheckBox.setSelected(UserFeignSettings.getInstance().isIconEnabled());
        controllerIconEnabledCheckBox.setSelected(UserRestControllerSettings.getInstance().isIconEnabled());
    }

    /**
     * Refresh the icons in the project, excludes the LineMarkerProvider
     */
    @Deprecated
    private void refreshIconsWithoutLineMarkerProvider() {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : projects) {
            ApplicationManager.getApplication().runWriteAction(() -> {
                VirtualFile[] openFiles = FileEditorManager.getInstance(project).getOpenFiles();
                // 刷新文件图标，不触发 LineMarkerProvider
                for (VirtualFile file : openFiles) {
                    file.refresh(true, false);
                }
                //刷新左侧的项目树中的文件图标，，不触发 LineMarkerProvider
                ProjectView projectView = ProjectView.getInstance(project);
                projectView.refresh();
                for (VirtualFile file : openFiles) {
                    //第一个参数是要选中的 VirtualFile，第二个参数是目标 VirtualFile（一般相同），第三个参数表示是否聚焦。
                    projectView.select(file, file, true);
                }
            });
        }
    }

    /**
     * 显示重启提示对话框
     */
    private void showRestartDialog() {
        int option = JOptionPane.showConfirmDialog(
                null,
                "Configuration has been applied. Please restart(manually) the IDE for the changes to take effect.",
                "Restart Required",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );

        if (option == JOptionPane.OK_OPTION) {
            // 这里可以添加触发重启的逻辑
            ApplicationManager.getApplication().restart();
        }
    }
}
