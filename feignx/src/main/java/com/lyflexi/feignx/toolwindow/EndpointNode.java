package com.lyflexi.feignx.toolwindow;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

/**
 * 端点侧边栏树节点数据载体。
 * <p>
 * 仅作为 {@code DefaultMutableTreeNode} 的 userObject,不持有任何历史代码依赖,
 * 树结构展示与渲染逻辑完全收敛在 toolwindow 包内。
 * <p>
 * 注意:文本/tooltip 均为扫描阶段预计算好的纯字符串,树重建(EDT)阶段不再访问 PSI,避免 read-action 校验失败。
 *
 * @Author: lyflexi
 * @project: feignx-plugin
 */
public class EndpointNode {

    /**
     * 节点类型:分组 / 类 / 请求方法 / 空占位
     */
    public enum Kind {
        GROUP, CLASS, METHOD, EMPTY
    }

    /**
     * 端点分组类型:SpringMVC / OpenFeign / SpringBoot,决定类节点左侧的 gutter 图标
     */
    public enum GroupType {
        SPRING_MVC, OPEN_FEIGN, SPRING_BOOT
    }

    private final Kind kind;
    private final String text;
    private final String tooltip;
    private final GroupType groupType;
    private final PsiClass psiClass;
    private final PsiMethod psiMethod;

    private EndpointNode(Kind kind, String text, String tooltip, GroupType groupType, PsiClass psiClass, PsiMethod psiMethod) {
        this.kind = kind;
        this.text = text;
        this.tooltip = tooltip;
        this.groupType = groupType;
        this.psiClass = psiClass;
        this.psiMethod = psiMethod;
    }

    public static EndpointNode group(String title, GroupType groupType) {
        return new EndpointNode(Kind.GROUP, title, null, groupType, null, null);
    }

    public static EndpointNode clazz(PsiClass psiClass, String text, String tooltip, GroupType groupType) {
        return new EndpointNode(Kind.CLASS, text, tooltip, groupType, psiClass, null);
    }

    public static EndpointNode method(PsiMethod psiMethod, String text, String tooltip, GroupType groupType) {
        return new EndpointNode(Kind.METHOD, text, tooltip, groupType, null, psiMethod);
    }

    public static EndpointNode empty(String text) {
        return new EndpointNode(Kind.EMPTY, text, null, GroupType.SPRING_MVC, null, null);
    }

    public Kind getKind() {
        return kind;
    }

    public String getText() {
        return text;
    }

    public String getTooltip() {
        return tooltip;
    }

    public GroupType getGroupType() {
        return groupType;
    }

    public PsiClass getPsiClass() {
        return psiClass;
    }

    public PsiMethod getPsiMethod() {
        return psiMethod;
    }
}
