package com.lyflexi.feignx.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.lyflexi.feignx.entity.HttpMappingInfo;
import com.lyflexi.feignx.utils.JsonFormatter;
import com.lyflexi.feignx.utils.StringUtil;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 端点侧边栏右侧面板:左右分栏。
 * <p>
 * 左块:当前选中请求方法自动生成的标准 HTTP 脚本模板(可编辑),顶部提供基础地址输入框与「执行」按钮,
 * 点击执行后由 {@link HttpScriptExecutor} 发送请求;
 * 右块:展示最近一次请求的响应体(状态码 + 响应头 + 响应体)。
 * <p>
 * 线程模型:脚本生成在 ReadAction(EDT)内解析 PSI;HTTP 执行在后台线程,结果回调 EDT 渲染。
 *
 * @Author: lyflexi
 * @project: feignx-plugin
 */
public class EndpointRequestPanel extends JPanel {

    private final Project project;
    private final JTextArea scriptArea;
    private final JTextArea responseArea;
    private final JButton executeButton;
    private final JButton formatScriptButton;
    private final JButton formatResponseButton;
    private final JButton historyButton;
    private final JBLabel responseStatusLabel;
    private final HttpHistoryManager historyManager;

    public EndpointRequestPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        this.historyManager = new HttpHistoryManager(project);

        executeButton = new JButton("执行", AllIcons.Actions.Execute);
        executeButton.setToolTipText("一键执行左侧 HTTP 脚本");
        executeButton.addActionListener(e -> executeScript());

        formatScriptButton = createFormatButton("格式化请求体 JSON");
        formatScriptButton.addActionListener(e -> formatScriptBody());

        formatResponseButton = createFormatButton("格式化响应体 JSON");
        formatResponseButton.addActionListener(e -> formatResponseBody());

        historyButton = new JButton("历史请求");
        historyButton.setToolTipText("查看当前工程的历史请求记录");
        historyButton.setFocusable(false);
        historyButton.addActionListener(e -> showHistoryPopup());

        scriptArea = createTextArea(true);
        responseArea = createTextArea(false);

        JPanel leftBlock = new JPanel(new BorderLayout(0, 4));
        leftBlock.add(createScriptHeader(), BorderLayout.NORTH);
        JBScrollPane scriptScroll = new JBScrollPane(scriptArea);
        scriptScroll.setBorder(JBUI.Borders.empty());
        leftBlock.add(scriptScroll, BorderLayout.CENTER);

        JPanel rightBlock = new JPanel(new BorderLayout(0, 4));
        rightBlock.setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0));
        JPanel responseHeader = new JPanel(new BorderLayout());
        responseHeader.setBorder(JBUI.Borders.empty(2, 4));
        JPanel responseLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        responseLeft.add(new JBLabel("响应体"));
        responseLeft.add(formatResponseButton);
        responseLeft.add(historyButton);
        responseHeader.add(responseLeft, BorderLayout.WEST);
        responseStatusLabel = new JBLabel(" ");
        responseStatusLabel.setForeground(UIUtil.getInactiveTextColor());
        responseHeader.add(responseStatusLabel, BorderLayout.EAST);
        rightBlock.add(responseHeader, BorderLayout.NORTH);
        JBScrollPane responseScroll = new JBScrollPane(responseArea);
        responseScroll.setBorder(JBUI.Borders.empty());
        rightBlock.add(responseScroll, BorderLayout.CENTER);

        JBSplitter splitPane = new JBSplitter(false, 0.5f);
        splitPane.setDividerWidth(8);
        splitPane.setFirstComponent(leftBlock);
        splitPane.setSecondComponent(rightBlock);

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createScriptHeader() {
        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.setBorder(JBUI.Borders.empty(2, 4));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.add(new JBLabel("HTTP 脚本"));
        left.add(formatScriptButton);
        left.add(executeButton);
        header.add(left, BorderLayout.WEST);
        return header;
    }

    private static JButton createFormatButton(String tooltip) {
        JButton button = new JButton("格式化");
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        return button;
    }

    private static JTextArea createTextArea(boolean editable) {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(editable);
        textArea.setLineWrap(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, UIUtil.getLabelFont().getSize()));
        textArea.setBorder(JBUI.Borders.empty(4, 6));
        return textArea;
    }

    /**
     * 根据选中的请求方法生成 HTTP 脚本模板,填充至左块脚本编辑区,并清空右块响应体。
     */
    public void showScript(HttpMappingInfo info) {
        if (info == null || project.isDisposed()) {
            return;
        }
        if (DumbService.isDumb(project)) {
            return;
        }
        String script = ReadAction.compute(() -> HttpScriptGenerator.generate(info));
        scriptArea.setText(script == null ? "" : script);
        scriptArea.setCaretPosition(0);
        responseArea.setText("");
        responseStatusLabel.setText(" ");
    }

    /**
     * 清空脚本编辑区与响应体(未选中请求方法时调用)。
     */
    public void clear() {
        scriptArea.setText("");
        responseArea.setText("");
        responseStatusLabel.setText(" ");
    }

    /**
     * 展示当前工程的历史请求记录弹窗(支持输入过滤),选中某条记录后将其请求脚本与响应覆盖到当前面板。
     */
    private void showHistoryPopup() {
        List<HttpHistoryManager.HttpHistoryRecord> records = historyManager.load();
        if (records.isEmpty()) {
            responseStatusLabel.setText("暂无历史请求记录");
            return;
        }
        JBPopup popup = JBPopupFactory.getInstance()
                .createPopupChooserBuilder(records)
                .setTitle("历史请求")
                .setMovable(true)
                .setRenderer(new HistoryListCellRenderer())
                .setNamerForFiltering(record -> record.getRequestPath() + " " + record.getTimestamp())
                .setItemChosenCallback(record -> applyHistoryRecord((HttpHistoryManager.HttpHistoryRecord) record))
                .createPopup();
        popup.showUnderneathOf(historyButton);
    }

    /**
     * 历史请求列表渲染器:主行展示请求路径,次行灰色展示时间戳。
     */
    private static final class HistoryListCellRenderer extends ColoredListCellRenderer<Object> {
        @Override
        protected void customizeCellRenderer(JList<?> list, Object value, int index, boolean selected, boolean hasFocus) {
            if (!(value instanceof HttpHistoryManager.HttpHistoryRecord)) {
                append(value == null ? "" : value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                return;
            }
            HttpHistoryManager.HttpHistoryRecord record = (HttpHistoryManager.HttpHistoryRecord) value;
            setIcon(AllIcons.Actions.Refresh);
            append(record.getRequestPath(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            append("  " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(record.getTimestamp())),
                    SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
    }

    /**
     * 用历史记录覆盖当前请求脚本与响应数据。
     */
    private void applyHistoryRecord(HttpHistoryManager.HttpHistoryRecord record) {
        if (record == null) {
            return;
        }
        scriptArea.setText(record.getScript());
        scriptArea.setCaretPosition(0);
        responseArea.setText(record.getResponse());
        responseArea.setCaretPosition(0);
        responseStatusLabel.setText("已载入历史请求: " + record.getRequestPath());
    }

    /**
     * 格式化脚本区的请求体 JSON(空行之后的正文部分),格式化为两空格缩进。
     */
    private void formatScriptBody() {
        String script = scriptArea.getText();
        if (StringUtil.isBlank(script)) {
            return;
        }
        int bodyStart = findBodyStart(script);
        if (bodyStart < 0) {
            responseStatusLabel.setText("未找到请求体");
            return;
        }
        String body = script.substring(bodyStart).trim();
        String formatted = JsonFormatter.format(body);
        if (formatted == null) {
            responseStatusLabel.setText("请求体不是合法 JSON");
            return;
        }
        scriptArea.setText(script.substring(0, bodyStart) + "\n" + formatted);
        responseStatusLabel.setText("请求体已格式化");
    }

    /**
     * 格式化响应区「响应体」部分的 JSON。
     */
    private void formatResponseBody() {
        String text = responseArea.getText();
        if (StringUtil.isBlank(text)) {
            return;
        }
        int bodyStart = findResponseBodyStart(text);
        if (bodyStart < 0) {
            responseStatusLabel.setText("未找到响应体");
            return;
        }
        String body = text.substring(bodyStart).trim();
        String formatted = JsonFormatter.format(body);
        if (formatted == null) {
            responseStatusLabel.setText("响应体不是合法 JSON");
            return;
        }
        responseArea.setText(text.substring(0, bodyStart) + "\n" + formatted);
        responseArea.setCaretPosition(0);
        responseStatusLabel.setText("响应体已格式化");
    }

    /**
     * 定位 HTTP 脚本中请求体起始位置(首个空行之后)。
     */
    private static int findBodyStart(String script) {
        int index = 0;
        while (index < script.length()) {
            int lineEnd = script.indexOf('\n', index);
            if (lineEnd < 0) {
                return -1;
            }
            String line = script.substring(index, lineEnd).trim();
            if (line.isEmpty()) {
                return lineEnd + 1;
            }
            index = lineEnd + 1;
        }
        return -1;
    }

    /**
     * 定位响应文本中「响应体」标题之后的位置。
     */
    private static int findResponseBodyStart(String text) {
        String marker = "=== 响应体 ===";
        int idx = text.lastIndexOf(marker);
        if (idx < 0) {
            return -1;
        }
        int lineEnd = text.indexOf('\n', idx);
        return lineEnd < 0 ? text.length() : lineEnd + 1;
    }

    /**
     * 执行左侧脚本:后台线程发送请求,EDT 回调渲染响应体。
     */
    private void executeScript() {
        if (project.isDisposed()) {
            return;
        }
        String script = scriptArea.getText();
        if (StringUtil.isBlank(script)) {
            responseStatusLabel.setText("脚本为空");
            return;
        }
        executeButton.setEnabled(false);
        responseStatusLabel.setText("请求中...");
        responseStatusLabel.setForeground(UIUtil.getLabelForeground());

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                HttpScriptExecutor.HttpExecutionResult result = HttpScriptExecutor.execute(script);
                String rendered = result.render();
                historyManager.save(script, rendered);
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed()) {
                        return;
                    }
                    responseArea.setText(rendered);
                    responseArea.setCaretPosition(0);
                    responseStatusLabel.setText("状态: " + result.getStatusCode());
                    executeButton.setEnabled(true);
                });
            } catch (Exception ex) {
                String failure = "请求失败:\n" + Objects.toString(ex.getMessage(), ex.toString());
                historyManager.save(script, failure);
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed()) {
                        return;
                    }
                    responseArea.setText(failure);
                    responseStatusLabel.setText("请求失败");
                    executeButton.setEnabled(true);
                });
            }
        });
    }
}
