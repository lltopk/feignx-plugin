package com.lyflexi.feignx.refresh;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.lyflexi.feignx.entity.HttpMappingInfo;
import com.lyflexi.feignx.utils.ControllerClassScanUtils;
import com.lyflexi.feignx.utils.FeignClassScanUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Feign/Controller 映射手动刷新器(Issue #25 兜底功能)
 * <p>
 * 设计上独立于任何 UI 入口(StatusBar 图标、未来的快捷键 / Action),
 * 任意调用方仅需调用 {@link #refreshAll(Project)} 即可触发一次全量刷新,
 * 与 StatusBar 展示层完全解耦,便于后续扩展新的入口。
 */
public final class FeignRefreshManager {

    private static final String NOTIFICATION_GROUP = "FeignClient Assistant";

    private FeignRefreshManager() {
    }

    /**
     * 手动刷新当前工程下所有 FeignClient 与 Controller 映射
     * <p>
     * 1. 基于 IntelliJ 注解索引全量扫描 Controller / Feign 接口,统计数量用于反馈;
     * 2. 重启 Daemon,强制所有 gutter 导航图标重新计算(兜底解决索引/缓存导致的跳转失效);
     * 3. 以通知形式告知扫描结果。
     * <p>
     * 扫描在后台线程执行,不阻塞 EDT。
     *
     * @param project 目标工程
     */
    public static void refreshAll(@NotNull Project project) {
        if (project.isDisposed()) {
            return;
        }
        if (DumbService.isDumb(project)) {
            notifyUser(project, "索引尚未加载完成,请稍后重试", NotificationType.WARNING);
            return;
        }
        new Task.Backgroundable(project, "刷新 Feign/Controller 映射", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (DumbService.isDumb(project)) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (!project.isDisposed()) {
                            notifyUser(project, "索引尚未加载完成,请稍后重试", NotificationType.WARNING);
                        }
                    });
                    return;
                }
                RefreshResult result = ReadAction.compute(() -> doScan(project));
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed()) {
                        return;
                    }
                    // 强制 gutter 导航图标重新计算
                    DaemonCodeAnalyzer.getInstance(project).restart();
                    notifyUser(project,
                            "刷新完成:扫描到 " + result.controllerClassCount + " 个 Controller("
                                    + result.controllerMethodCount + " 个接口方法),"
                                    + result.feignClassCount + " 个 FeignClient("
                                    + result.feignMethodCount + " 个接口方法)",
                            NotificationType.INFORMATION);
                });
            }
        }.queue();
    }

    /**
     * 执行全量扫描并统计,必须在读锁(ReadAction)内调用
     */
    private static RefreshResult doScan(Project project) {
        List<HttpMappingInfo> controllers = ControllerClassScanUtils.scanControllerPaths(project);
        List<HttpMappingInfo> feigns = FeignClassScanUtils.scanFeignInterfaces(project);
        RefreshResult result = new RefreshResult();
        result.controllerMethodCount = controllers.size();
        result.controllerClassCount = countClasses(controllers);
        result.feignMethodCount = feigns.size();
        result.feignClassCount = countClasses(feigns);
        return result;
    }

    private static long countClasses(List<HttpMappingInfo> infos) {
        return infos.stream()
                .map(HttpMappingInfo::getPsiMethod)
                .filter(Objects::nonNull)
                .map(psiMethod -> psiMethod.getContainingClass())
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private static void notifyUser(Project project, String content, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP)
                .createNotification(content, type)
                .notify(project);
    }

    private static final class RefreshResult {
        private long controllerClassCount;
        private int controllerMethodCount;
        private long feignClassCount;
        private int feignMethodCount;
    }
}
