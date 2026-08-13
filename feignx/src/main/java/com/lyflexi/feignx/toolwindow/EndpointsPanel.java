package com.lyflexi.feignx.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.lyflexi.feignx.constant.RestIcons;
import com.lyflexi.feignx.entity.HttpMappingInfo;
import com.lyflexi.feignx.utils.ControllerClassScanUtils;
import com.lyflexi.feignx.utils.FeignClassScanUtils;
import com.lyflexi.feignx.utils.StringUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * "feignx assistant endpoints" 侧边栏面板。
 * <p>
 * 独立实现,与历史 gutter/状态栏等代码零耦合:
 * 顶部工具条(刷新 + 路径模糊搜索 + 请求类型下拉过滤) + 端点树
 * (SpringMVC / OpenFeign / SpringBoot 三个分组,组下按类再按请求路径展开)。
 * <p>
 * 线程模型:全量扫描在后台线程的 ReadAction 内完成,并预计算好所有展示用的纯字符串快照;
 * 搜索/过滤触发的树重建(EDT)仅消费快照字符串,不再访问 PSI,规避 read-action 校验。
 *
 * @Author: lyflexi
 * @project: feignx-plugin
 */
public class EndpointsPanel extends JPanel {

    private static final String ALL_METHODS = "全部";
    private static final String ALL_ENDPOINT_TYPES = "全部";

    /**
     * SpringBoot 启动类专用图标(经典 Spring logo,与 spring.svg 一致)
     */
    private static final Icon SPRING_ICON = IconLoader.getIcon("/icons/spring.svg", EndpointsPanel.class);

    private static final String SPRING_BOOT_APPLICATION = "org.springframework.boot.autoconfigure.SpringBootApplication";

    private final Project project;
    private final JTree tree;
    private final DefaultTreeModel treeModel;
    private final SearchTextField searchField;
    private final JComboBox<String> methodCombo;
    private final JComboBox<String> endpointTypeCombo;
    private final JButton refreshButton;
    private final JBLabel statusLabel;

    /**
     * 最近一次全量扫描的纯字符串快照(EDT 树重建只读它,不碰 PSI)
     */
    private EndpointSnapshot snapshot = EndpointSnapshot.empty();

    public EndpointsPanel(Project project) {
        super(new BorderLayout());
        this.project = project;

        treeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
        tree = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        // 显式使用标准树背景,避免透明树透出灰色面板/视口背景形成"灰色遮罩"
        tree.setOpaque(true);
        tree.setBackground(UIUtil.getTreeBackground());
        tree.setCellRenderer(new EndpointTreeCellRenderer());
        installDoubleClickNavigation();

        JBScrollPane scrollPane = new JBScrollPane(tree);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(UIUtil.getTreeBackground());

        searchField = new SearchTextField();
        searchField.setPreferredSize(new Dimension(180, -1));
        searchField.setToolTipText("按路径模糊搜索");
        searchField.getTextEditor().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                rebuildTree();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                rebuildTree();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                rebuildTree();
            }
        });

        methodCombo = new JComboBox<>(new String[]{ALL_METHODS, "GET", "POST", "PUT", "DELETE"});
        methodCombo.setToolTipText("按请求类型过滤");
        methodCombo.addActionListener(e -> rebuildTree());

        endpointTypeCombo = new JComboBox<>(new String[]{ALL_ENDPOINT_TYPES, "SpringBoot", "SpringMVC", "OpenFeign"});
        endpointTypeCombo.setToolTipText("按端点类型过滤");
        endpointTypeCombo.addActionListener(e -> rebuildTree());

        refreshButton = createToolbarButton(AllIcons.Actions.Refresh, "刷新端点列表", e -> refresh());

        JButton expandAllButton = createToolbarButton(AllIcons.Actions.Expandall, "全部展开", e -> expandAllNodes());

        JButton collapseAllButton = createToolbarButton(AllIcons.Actions.Collapseall, "全部收起", e -> collapseAllNodes());

        JPanel topBar = new JPanel(new BorderLayout(6, 0));
        JPanel leftBar = new JPanel();
        leftBar.setLayout(new BoxLayout(leftBar, BoxLayout.Y_AXIS));
        leftBar.add(Box.createVerticalGlue());
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonRow.add(refreshButton);
        buttonRow.add(expandAllButton);
        buttonRow.add(collapseAllButton);
        leftBar.add(buttonRow);
        leftBar.add(Box.createVerticalGlue());
        topBar.add(leftBar, BorderLayout.WEST);
        topBar.add(searchField, BorderLayout.CENTER);

        JPanel rightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        rightBar.add(new JBLabel("HttpType:"));
        rightBar.add(methodCombo);
        rightBar.add(new JBLabel("EndpointType:"));
        rightBar.add(endpointTypeCombo);
        topBar.add(rightBar, BorderLayout.EAST);

        statusLabel = new JBLabel(" ");
        statusLabel.setBorder(JBUI.Borders.empty(2, 6));

        add(topBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        refresh();
    }

    /**
     * 创建紧凑的工具栏图标按钮:宽度保持窄小(18px,整体块窄),高度取常规按钮高度(24px)避免过小。
     */
    private static JButton createToolbarButton(Icon icon, String tooltip, ActionListener actionListener) {
        JButton button = new JButton(icon);
        button.setToolTipText(tooltip);
        button.setMargin(JBUI.insets(0));
        button.setFocusable(false);
        button.setBorder(JBUI.Borders.empty(1));
        Dimension size = new Dimension(JBUI.scale(18), JBUI.scale(24));
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.addActionListener(actionListener);
        return button;
    }

    /**
     * 手动刷新:后台线程全量扫描工程内 Controller / FeignClient / SpringBoot 启动类,并构建纯字符串快照。
     * 索引未就绪时挂起等待,dumb 模式下扫描结果为空(与历史逻辑一致,不会抛异常)。
     */
    private void refresh() {
        if (project.isDisposed()) {
            return;
        }
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(this::refresh);
            return;
        }
        refreshButton.setEnabled(false);
        statusLabel.setText("正在扫描端点...");
        new Task.Backgroundable(project, "扫描 feignx 端点", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (DumbService.isDumb(project)) {
                    return;
                }
                EndpointSnapshot result = ReadAction.compute(() -> buildSnapshot(project));
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed()) {
                        return;
                    }
                    snapshot = result;
                    refreshButton.setEnabled(true);
                    rebuildTree();
                });
            }
        }.queue();
    }

    /**
     * 后台线程(ReadAction 内)执行全量扫描,并预计算树展示所需的全部纯字符串。
     */
    private static EndpointSnapshot buildSnapshot(Project project) {
        List<EndpointClassData> controllers = groupEndpoints(ControllerClassScanUtils.scanControllerPaths(project));
        List<EndpointClassData> feigns = groupEndpoints(FeignClassScanUtils.scanFeignInterfaces(project));
        List<EndpointClassData> springBoots = groupSpringBootClasses(project);
        return new EndpointSnapshot(controllers, feigns, springBoots,
                countMethods(controllers), countMethods(feigns));
    }

    /**
     * 按类分组,类按全限定名排序,类内方法按路径排序;同时预计算展示用的类名/全限定名字符串。
     */
    private static List<EndpointClassData> groupEndpoints(List<HttpMappingInfo> infos) {
        Map<PsiClass, List<HttpMappingInfo>> byClass = new TreeMap<>(Comparator.comparing(
                c -> c.getQualifiedName() != null ? c.getQualifiedName() : c.getName(),
                Comparator.nullsLast(Comparator.naturalOrder())));

        for (HttpMappingInfo info : infos) {
            PsiMethod psiMethod = info.getPsiMethod();
            if (psiMethod == null || !psiMethod.isValid()) {
                continue;
            }
            PsiClass psiClass = psiMethod.getContainingClass();
            if (psiClass == null) {
                continue;
            }
            byClass.computeIfAbsent(psiClass, k -> new ArrayList<>()).add(info);
        }

        List<EndpointClassData> result = new ArrayList<>();
        for (Map.Entry<PsiClass, List<HttpMappingInfo>> entry : byClass.entrySet()) {
            PsiClass psiClass = entry.getKey();
            String qualifiedName = psiClass.getQualifiedName();
            String name = psiClass.getName();
            String displayName = name == null ? qualifiedName : name;
            List<HttpMappingInfo> methods = entry.getValue().stream()
                    .sorted(Comparator.comparing(m -> m.getPath() == null ? "" : m.getPath()))
                    .collect(Collectors.toList());
            result.add(new EndpointClassData(psiClass, displayName, qualifiedName, methods));
        }
        return result;
    }

    /**
     * 扫描并分组所有 SpringBoot 启动类(@SpringBootApplication),按全限定名排序,无请求路径子节点。
     */
    private static List<EndpointClassData> groupSpringBootClasses(Project project) {
        List<PsiClass> classes = scanSpringBootClasses(project);
        List<EndpointClassData> result = new ArrayList<>();
        for (PsiClass psiClass : classes) {
            if (psiClass == null || !psiClass.isValid()) {
                continue;
            }
            String qualifiedName = psiClass.getQualifiedName();
            String name = psiClass.getName();
            String displayName = name == null ? qualifiedName : name;
            result.add(new EndpointClassData(psiClass, displayName, qualifiedName, Collections.emptyList()));
        }
        result.sort(Comparator.comparing(cd -> cd.qualifiedName == null ? "" : cd.qualifiedName));
        return result;
    }

    /**
     * 基于 IntelliJ 注解索引扫描标注 @SpringBootApplication 的启动类(仅工程源码范围)
     */
    private static List<PsiClass> scanSpringBootClasses(Project project) {
        if (DumbService.isDumb(project)) {
            return Collections.emptyList();
        }
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        PsiClass annotationClass = facade.findClass(SPRING_BOOT_APPLICATION, GlobalSearchScope.allScope(project));
        if (annotationClass == null) {
            return Collections.emptyList();
        }
        Collection<PsiClass> found = AnnotatedElementsSearch
                .searchPsiClasses(annotationClass, GlobalSearchScope.projectScope(project))
                .findAll();
        return new ArrayList<>(found);
    }

    private static int countMethods(List<EndpointClassData> classes) {
        int count = 0;
        for (EndpointClassData classData : classes) {
            count += classData.methods.size();
        }
        return count;
    }

    /**
     * 按当前搜索关键字 + 请求类型过滤重建端点树。
     * 仅消费快照中的纯字符串(路径/请求类型/类名),不访问任何 PSI,可安全运行在 EDT。
     */
    private void rebuildTree() {
        String keyword = searchField.getText().trim().toLowerCase(Locale.ROOT);
        String methodFilter = (String) methodCombo.getSelectedItem();
        String endpointType = (String) endpointTypeCombo.getSelectedItem();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        // SpringBoot 分组排在最上面
        if (showGroup(endpointType, EndpointNode.GroupType.SPRING_BOOT)) {
            root.add(buildSpringBootGroup(snapshot.springBootClasses, keyword));
        }
        if (showGroup(endpointType, EndpointNode.GroupType.SPRING_MVC)) {
            root.add(buildGroup("SpringMVC", EndpointNode.GroupType.SPRING_MVC,
                    snapshot.controllerClasses, keyword, methodFilter));
        }
        if (showGroup(endpointType, EndpointNode.GroupType.OPEN_FEIGN)) {
            root.add(buildGroup("OpenFeign", EndpointNode.GroupType.OPEN_FEIGN,
                    snapshot.feignClasses, keyword, methodFilter));
        }
        treeModel.setRoot(root);

        int matched = 0;
        for (int i = 0; i < root.getChildCount(); i++) {
            matched += countMatchedClasses((DefaultMutableTreeNode) root.getChildAt(i));
            tree.expandRow(i);
        }
        statusLabel.setText("共 " + matched + " 个匹配类");
        statusLabel.setToolTipText("SpringMVC " + snapshot.controllerCount
                + " / OpenFeign " + snapshot.feignCount
                + " / SpringBoot " + snapshot.springBootClasses.size());
    }

    /**
     * 判断当前 EndpointType 下拉框选择是否展示指定分组
     */
    private static boolean showGroup(String endpointType, EndpointNode.GroupType groupType) {
        if (endpointType == null || ALL_ENDPOINT_TYPES.equals(endpointType)) {
            return true;
        }
        switch (groupType) {
            case SPRING_BOOT:
                return "SpringBoot".equals(endpointType);
            case SPRING_MVC:
                return "SpringMVC".equals(endpointType);
            case OPEN_FEIGN:
            default:
                return "OpenFeign".equals(endpointType);
        }
    }

    private DefaultMutableTreeNode buildGroup(String title, EndpointNode.GroupType groupType, List<EndpointClassData> classes,
                                              String keyword, String methodFilter) {
        DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(EndpointNode.group(title, groupType));
        for (EndpointClassData classData : classes) {
            List<HttpMappingInfo> methods = classData.methods.stream()
                    .filter(m -> matches(m, keyword, methodFilter))
                    .collect(Collectors.toList());
            if (methods.isEmpty()) {
                continue;
            }
            DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(
                    EndpointNode.clazz(classData.psiClass, classData.displayName, classData.qualifiedName, groupType));
            for (HttpMappingInfo method : methods) {
                classNode.add(new DefaultMutableTreeNode(
                        EndpointNode.method(method.getPsiMethod(), displayMethod(method), method.getPath(), groupType)));
            }
            groupNode.add(classNode);
        }

        if (groupNode.getChildCount() == 0) {
            groupNode.add(new DefaultMutableTreeNode(EndpointNode.empty("无匹配端点")));
        }
        return groupNode;
    }

    /**
     * SpringBoot 分组:启动类无请求路径,按关键字匹配类名/全限定名即可(请求类型过滤不适用)。
     */
    private DefaultMutableTreeNode buildSpringBootGroup(List<EndpointClassData> classes, String keyword) {
        DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(
                EndpointNode.group("SpringBoot", EndpointNode.GroupType.SPRING_BOOT));
        boolean keywordBlank = StringUtil.isBlank(keyword);
        for (EndpointClassData classData : classes) {
            String searchable = (classData.displayName + " " + classData.qualifiedName).toLowerCase(Locale.ROOT);
            if (!keywordBlank && !searchable.contains(keyword)) {
                continue;
            }
            groupNode.add(new DefaultMutableTreeNode(
                    EndpointNode.clazz(classData.psiClass, classData.displayName, classData.qualifiedName,
                            EndpointNode.GroupType.SPRING_BOOT)));
        }

        if (groupNode.getChildCount() == 0) {
            groupNode.add(new DefaultMutableTreeNode(EndpointNode.empty("无匹配端点")));
        }
        return groupNode;
    }

    private static int countMatchedClasses(DefaultMutableTreeNode groupNode) {
        int count = 0;
        for (int i = 0; i < groupNode.getChildCount(); i++) {
            Object userObject = ((DefaultMutableTreeNode) groupNode.getChildAt(i)).getUserObject();
            if (userObject instanceof EndpointNode && ((EndpointNode) userObject).getKind() == EndpointNode.Kind.CLASS) {
                count++;
            }
        }
        return count;
    }

    /**
     * 路径模糊(contains,忽略大小写) + 请求类型过滤。
     * 只读 HttpMappingInfo 的纯字符串字段(path / requestMethod),不触碰 PSI。
     */
    private static boolean matches(HttpMappingInfo info, String keyword, String methodFilter) {
        String path = info.getPath() == null ? "" : info.getPath();
        if (StringUtil.isNotBlank(keyword) && !path.toLowerCase(Locale.ROOT).contains(keyword)) {
            return false;
        }
        if (methodFilter != null && !ALL_METHODS.equals(methodFilter)) {
            String requestMethod = info.getRequestMethod();
            if (requestMethod == null || !methodFilter.equalsIgnoreCase(requestMethod)) {
                return false;
            }
        }
        return true;
    }

    private static String displayMethod(HttpMappingInfo info) {
        String requestMethod = info.getRequestMethod();
        String tag;
        if (requestMethod == null || "REQUEST".equals(requestMethod)) {
            tag = "ALL";
        } else {
            tag = requestMethod;
        }
        return tag + "  " + (info.getPath() == null ? "" : info.getPath());
    }

    private void installDoubleClickNavigation() {
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path == null) {
                        return;
                    }
                    Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                    if (!(userObject instanceof EndpointNode)) {
                        return;
                    }
                    EndpointNode node = (EndpointNode) userObject;
                    PsiElement target = null;
                    if (node.getKind() == EndpointNode.Kind.METHOD) {
                        target = node.getPsiMethod();
                    } else if (node.getKind() == EndpointNode.Kind.CLASS) {
                        target = node.getPsiClass();
                    }
                    if (target != null) {
                        navigateTo(target);
                    }
                }
            }
        });
    }

    /**
     * 跳转到 PSI 元素源码。PSI 读取(valid/containingFile/textOffset)包裹在 ReadAction 内,导航本身在 EDT 上执行。
     */
    private static void navigateTo(PsiElement element) {
        Project targetProject = element.getProject();
        VirtualFile[] file = {null};
        int[] offset = {0};
        ApplicationManager.getApplication().runReadAction(() -> {
            if (element.isValid()) {
                file[0] = element.getContainingFile().getVirtualFile();
                offset[0] = element.getTextOffset();
            }
        });
        if (file[0] == null) {
            return;
        }
        PsiNavigationSupport.getInstance()
                .createNavigatable(targetProject, file[0], offset[0])
                .navigate(true);
    }

    /**
     * 全部展开:展开树中的所有节点
     */
    private void expandAllNodes() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    /**
     * 全部收起:收起树中的所有节点(含三个分组节点)
     */
    private void collapseAllNodes() {
        for (int i = tree.getRowCount() - 1; i >= 0; i--) {
            tree.collapseRow(i);
        }
    }

    /**
     * 扫描快照:三类分组下的 类 -> 请求方法 树数据(展示字符串已在后台线程预计算)
     */
    private static final class EndpointSnapshot {
        private static final EndpointSnapshot EMPTY =
                new EndpointSnapshot(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 0, 0);

        private final List<EndpointClassData> controllerClasses;
        private final List<EndpointClassData> feignClasses;
        private final List<EndpointClassData> springBootClasses;
        private final int controllerCount;
        private final int feignCount;

        private EndpointSnapshot(List<EndpointClassData> controllerClasses, List<EndpointClassData> feignClasses,
                                 List<EndpointClassData> springBootClasses, int controllerCount, int feignCount) {
            this.controllerClasses = controllerClasses;
            this.feignClasses = feignClasses;
            this.springBootClasses = springBootClasses;
            this.controllerCount = controllerCount;
            this.feignCount = feignCount;
        }

        private static EndpointSnapshot empty() {
            return EMPTY;
        }
    }

    /**
     * 单个端点类的展示数据:保留 PsiClass 用于双击跳转,展示文本为预计算字符串
     */
    private static final class EndpointClassData {
        private final PsiClass psiClass;
        private final String displayName;
        private final String qualifiedName;
        private final List<HttpMappingInfo> methods;

        private EndpointClassData(PsiClass psiClass, String displayName, String qualifiedName,
                                  List<HttpMappingInfo> methods) {
            this.psiClass = psiClass;
            this.displayName = displayName;
            this.qualifiedName = qualifiedName;
            this.methods = methods;
        }
    }

    /**
     * 端点树渲染器:类节点左侧按分组类型显示 gutter 图标。
     * SpringMVC -> jumpAction_controller.svg;OpenFeign -> jumpAction_feign.svg;SpringBoot -> spring.svg。
     */
    private static final class EndpointTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode) {
                Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                if (userObject instanceof EndpointNode) {
                    EndpointNode node = (EndpointNode) userObject;
                    setText(node.getText());
                    setToolTipText(node.getTooltip());
                    switch (node.getKind()) {
                        case GROUP:
                            setIcon(AllIcons.Nodes.Folder);
                            setFont(getFont().deriveFont(Font.BOLD));
                            break;
                        case CLASS:
                            setIcon(iconForGroup(node.getGroupType()));
                            setFont(tree.getFont());
                            break;
                        case METHOD:
                            setIcon(AllIcons.Nodes.Method);
                            setFont(tree.getFont());
                            break;
                        case EMPTY:
                            setIcon(null);
                            setFont(tree.getFont());
                            setForeground(UIUtil.getInactiveTextColor());
                            break;
                        default:
                            break;
                    }
                }
            }
            return this;
        }

        private static Icon iconForGroup(EndpointNode.GroupType groupType) {
            switch (groupType) {
                case OPEN_FEIGN:
                    return RestIcons.STATEMENT_LINE_FEIGN_ICON;
                case SPRING_BOOT:
                    return SPRING_ICON;
                case SPRING_MVC:
                default:
                    return RestIcons.STATEMENT_LINE_CONTROLLER_ICON;
            }
        }
    }
}
