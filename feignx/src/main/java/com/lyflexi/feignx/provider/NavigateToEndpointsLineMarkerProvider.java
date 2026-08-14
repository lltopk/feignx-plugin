package com.lyflexi.feignx.provider;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.GutterName;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.lyflexi.feignx.constant.RestIcons;
import com.lyflexi.feignx.entity.HttpMappingInfo;
import com.lyflexi.feignx.toolwindow.EndpointsPanel;
import com.lyflexi.feignx.utils.AnnotationParserUtils;
import com.lyflexi.feignx.utils.ControllerClassScanUtils;
import com.lyflexi.feignx.utils.FeignClassScanUtils;
import com.lyflexi.feignx.utils.ProjectUtils;
import com.lyflexi.feignx.utils.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        if (null == element) {
            return null;
        }
        if (!ProjectUtils.isBizElement(element)) {
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
            mappingInfo = ControllerClassScanUtils.controllerOfPsiMethod(method.getContainingClass(), project, method);
        } else {
            mappingInfo = FeignClassScanUtils.feignOfPsiMethod(method.getContainingClass(), method);
        }
        if (Objects.isNull(mappingInfo)) {
            return null;
        }
        String path = mappingInfo.getPath();
        if (StringUtil.isBlank(path)) {
            return null;
        }

        GutterIconNavigationHandler<PsiElement> handler = (mouseEvent, elt) ->
                navigateToEndpoints(project, path);

        return new LineMarkerInfo<>(
                restfulAnnotation,
                restfulAnnotation.getTextRange(),
                RestIcons.STATEMENT_LINE_OPEN_API_ICON,
                psi -> "Show In Endpoints Panel: " + path,
                handler,
                GutterIconRenderer.Alignment.RIGHT,
                () -> "Show In Endpoints Panel"
        );
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
}
