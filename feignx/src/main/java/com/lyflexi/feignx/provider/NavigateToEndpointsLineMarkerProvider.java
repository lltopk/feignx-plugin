package com.lyflexi.feignx.provider;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.GutterName;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.lyflexi.feignx.constant.RestIcons;
import com.lyflexi.feignx.entity.HttpMappingInfo;
import com.lyflexi.feignx.resolver.ControllerMappingResolver;
import com.lyflexi.feignx.resolver.FeignMappingResolver;
import com.lyflexi.feignx.toolwindow.EndpointsPanel;
import com.lyflexi.feignx.utils.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.StringSelection;
import java.util.Objects;

/**
 * 统一的「跳转到 Endpoints 面板」gutter 导航提供器。
 * <p>
 * 取代原 CopyControllerUrlLineMarkerProvider / CopyFeignUrlLineMarkerProvider(复制 URL + 右下角通知)。
 * 对 Controller 与 Feign 方法统一挂载一个 gutter 图标,点击后激活端点侧边栏,
 * 并定位/选中对应请求路径的树节点。
 * <p>
 * 图标仍挂在 Restful 注解旁(与历史逻辑一致,回车/注释编辑时更稳定)。
 *
 * @Author: lyflexi
 * @project: feignx-plugin
 */
public class NavigateToEndpointsLineMarkerProvider extends LineMarkerProviderDescriptor {

    private static final String TOOL_WINDOW_ID = "FeignClient Assistant With RequestX Endpoints";

    private static final String NOTIFICATION_GROUP_ID = "FeignClient Assistant With RequestX";

    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        if (null == element) {
            return null;
        }
        if (!BizChecker.isBizElement(element)) {
            return null;
        }
        Project project = element.getProject();
        if (DumbService.isDumb(project)) {
            return null;
        }
        if (!(element instanceof PsiMethod)) {
            return null;
        }

        PsiMethod method = (PsiMethod) element;
        if (null == method || !method.isValid()) {
            return null;
        }

        // 仅处理 Controller 或 Feign 方法
        boolean isController = AnnotationParserUtils.isElementWithinController(method);
        boolean isFeign = AnnotationParserUtils.isElementWithinFeign(element);
        if (!isController && !isFeign) {
            return null;
        }

        PsiAnnotation restfulAnnotation = AnnotationParserUtils.findRestfulAnnotation(method);
        if (Objects.isNull(restfulAnnotation)) {
            return null;
        }

        // 计算该方法的完整请求路径(与 EndpointsPanel 树节点 HttpMappingInfo.path 一致)
        HttpMappingInfo mappingInfo;
        if (isController) {
            mappingInfo = ControllerMappingResolver.controllerOfPsiMethod(method.getContainingClass(), project, method);
        } else {
            mappingInfo = FeignMappingResolver.feignOfPsiMethod(method.getContainingClass(), method);
        }
        if (Objects.isNull(mappingInfo)) {
            return null;
        }
        String path = mappingInfo.getPath();
        if (StringUtil.isBlank(path)) {
            return null;
        }

        // Issue #21:复制 URL 时拼接 @RequestParam query 串,如 /user/list?code=
        String urlWithParams = AnnotationParserUtils.appendRequestParams(path, AnnotationParserUtils.extractRequestParams(method));

        GutterIconNavigationHandler<PsiElement> handler = (mouseEvent, elt) ->
                navigateToEndpoints(project, path);

        // 左键单击跳转 Endpoints 面板,右键弹出功能菜单(复制完整请求路径 / 在 Endpoints 面板中显示)
        return new EndpointsLineMarkerInfo(restfulAnnotation, project, path, urlWithParams, handler);
    }

    /**
     * 激活端点侧边栏,并定位到指定路径的请求节点。
     */
    private static void navigateToEndpoints(Project project, String path) {
        if (project.isDisposed()) {
            return;
        }
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow == null) {
            return;
        }
        toolWindow.show(() -> {
            EndpointsPanel panel = EndpointsPanel.getInstance(project);
            if (panel != null) {
                panel.navigateTo(path);
            }
        });
    }

    @Override
    public @Nullable @GutterName String getName() {
        return "Show In Endpoints Panel";
    }

    /**
     * 挂载在 Restful 注解旁的 gutter 图标。
     * 左键单击触发 {@link #handler}(跳转 Endpoints 面板);
     * 右键通过覆写 {@link #createGutterRenderer()} 提供功能菜单(复制请求路径 / 在 Endpoints 面板中显示)。
     */
    private static final class EndpointsLineMarkerInfo extends LineMarkerInfo<PsiElement> {

        private final Project project;
        private final String path;
        private final String urlWithParams;

        private final GutterIconNavigationHandler<PsiElement> handler;

        private EndpointsLineMarkerInfo(PsiAnnotation restfulAnnotation,
                                        Project project,
                                        String path,
                                        String urlWithParams,
                                        GutterIconNavigationHandler<PsiElement> handler) {
            super(restfulAnnotation,
                    restfulAnnotation.getTextRange(),
                    RestIcons.STATEMENT_LINE_OPEN_API_ICON,
                    psi -> "Show In Endpoints Panel: " + path,
                    handler,
                    GutterIconRenderer.Alignment.RIGHT,
                    () -> "Show In Endpoints Panel");
            this.project = project;
            this.path = path;
            this.urlWithParams = urlWithParams;
            this.handler = handler;
        }

        @Override
        public GutterIconRenderer createGutterRenderer() {
            if (myIcon == null) {
                return null; // 与父类契约一致:无图标时不生成 renderer
            }
            // 平台端 LineMarkersUtil.createOrReuseLineMarker 会对返回值直接 checkcast 为
            // LineMarkerInfo.LineMarkerGutterIconRenderer,因此必须继承该类而非 GutterIconRenderer
            return new EndpointsGutterIconRenderer(this, project, path, urlWithParams);
        }
    }

    /**
     * gutter 图标 renderer:左键导航走父类 {@link LineMarkerInfo.LineMarkerGutterIconRenderer} 默认行为,
     * 右键弹出功能菜单(复制请求路径 / 在 Endpoints 面板中显示)。
     */
    private static final class EndpointsGutterIconRenderer extends LineMarkerInfo.LineMarkerGutterIconRenderer<PsiElement> {

        private final Project project;
        private final String path;
        private final String urlWithParams;

        private EndpointsGutterIconRenderer(LineMarkerInfo<PsiElement> info,
                                            Project project,
                                            String path,
                                            String urlWithParams) {
            super(info);
            this.project = project;
            this.path = path;
            this.urlWithParams = urlWithParams;
        }

        @Override
        public ActionGroup getPopupMenuActions() {
            return new DefaultActionGroup(
                    new AnAction("复制请求路径 (Copy URL)") {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent event) {
                            CopyPasteManager.getInstance().setContents(new StringSelection(urlWithParams));
                            NotificationGroupManager.getInstance()
                                    .getNotificationGroup(NOTIFICATION_GROUP_ID)
                                    .createNotification("URL Copied To Clipboard:\n" + urlWithParams, NotificationType.INFORMATION)
                                    .notify(project);
                        }
                    },
                    new AnAction("在 Endpoints 面板中显示 (Show In Endpoints Panel)") {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent event) {
                            navigateToEndpoints(project, path);
                        }
                    }
            );
        }
    }
}
