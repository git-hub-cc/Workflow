package club.ppmc.workflow.service;

import club.ppmc.workflow.dto.FormDefinitionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author cc
 * @description 负责从 Word(.docx) 文档解析并生成表单定义的服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WordImportService {

    private final ObjectMapper objectMapper;

    // --- 【新增】定义用于文本模式识别的正则表达式 ---
    private static final Map<Pattern, String> TEXT_PATTERNS = new LinkedHashMap<>();

    static {
        // 匹配 "标签：_____" 或 "Label: ____" 形式
        TEXT_PATTERNS.put(Pattern.compile("(.+?)[：:]([\\s_]+)$"), "Input");
        // 匹配 "标签：" 或 "Label:" 后面紧跟换行符的情况
        TEXT_PATTERNS.put(Pattern.compile("(.+?)[：:]$"), "Input");
    }

    /**
     * 解析上传的 Word 文件并生成表单定义 DTO
     * @param file .docx 格式的文件
     * @return 解析后的表单定义
     * @throws IOException
     */
    public FormDefinitionResponse parseWordAndGenerateForm(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传的文件不能为空");
        }

        List<Map<String, Object>> fields = new ArrayList<>();
        String formName = StringUtils.stripFilenameExtension(file.getOriginalFilename());

        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {

            List<IBodyElement> bodyElements = document.getBodyElements();

            for (IBodyElement element : bodyElements) {
                if (element instanceof XWPFParagraph) {
                    processParagraph((XWPFParagraph) element, fields);
                } else if (element instanceof XWPFTable) {
                    processTable((XWPFTable) element, fields);
                }
            }
        }

        // 构建最终的响应对象
        FormDefinitionResponse response = new FormDefinitionResponse();
        response.setName(formName);
        try {
            Map<String, Object> schema = new HashMap<>();
            schema.put("fields", fields);
            response.setSchemaJson(objectMapper.writeValueAsString(schema));
        } catch (JsonProcessingException e) {
            log.error("序列化表单 schema 失败", e);
            throw new RuntimeException("生成表单定义JSON失败", e);
        }

        return response;
    }

    /**
     * 处理段落元素
     * @param paragraph 段落对象
     * @param fields    表单字段列表
     */
    private void processParagraph(XWPFParagraph paragraph, List<Map<String, Object>> fields) {
        String text = paragraph.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        // --- 【核心修改：优先进行模式匹配】 ---
        for (Map.Entry<Pattern, String> entry : TEXT_PATTERNS.entrySet()) {
            Matcher matcher = entry.getKey().matcher(text);
            if (matcher.find()) {
                String label = matcher.group(1).trim();
                if (!label.isEmpty()) {
                    // 如果匹配成功，创建一个 GridRow 来放置标签和输入框
                    List<Map<String, Object>> columns = List.of(
                            createGridColumnWithFields(6, List.of(createStaticTextComponent("<p style=\"text-align: right; font-weight: bold;\">" + label + "</p>", "p"))),
                            createGridColumnWithFields(18, List.of(createInputComponent(label)))
                    );
                    fields.add(createGridRow(columns));
                    return; // 成功匹配并处理后，不再执行后续逻辑
                }
            }
        }
        // --- 【修改结束】 ---


        // 简单的启发式规则：如果段落居中且有加粗，则认为是标题
        boolean isCentered = paragraph.getAlignment() == ParagraphAlignment.CENTER;
        boolean isBold = paragraph.getRuns().stream().anyMatch(XWPFRun::isBold);

        if (isCentered && isBold) {
            fields.add(createStaticTextComponent("<h1>" + text + "</h1>", "h1"));
        } else {
            // 否则视为普通说明性文本
            fields.add(createStaticTextComponent("<p>" + text + "</p>", "p"));
        }
    }

    /**
     * 处理表格元素
     * @param table  表格对象
     * @param fields 表单字段列表
     */
    private void processTable(XWPFTable table, List<Map<String, Object>> fields) {
        if (table.getRows() == null || table.getRows().isEmpty()) {
            log.warn("跳过一个没有行的空表格。");
            return;
        }

        for (XWPFTableRow row : table.getRows()) {
            List<XWPFTableCell> cells = row.getTableCells();
            int numCells = cells.size();

            // --- 【核心修改：支持2列和4列表格】 ---
            if (numCells == 2 || numCells == 4) {
                List<Map<String, Object>> gridColumns = new ArrayList<>();
                int span = 24 / numCells; // 计算每列的栅格宽度

                for (int i = 0; i < numCells; i += 2) {
                    if (i + 1 < numCells) {
                        String label = cells.get(i).getText().trim().replaceAll("[:：\\s]+$", "");

                        // 检查标签单元格是否有内容，并且值单元格是否基本为空或包含占位符
                        String valueCellText = cells.get(i + 1).getText().trim();
                        if (!label.isEmpty()) {
                            // 标签列
                            gridColumns.add(createGridColumnWithFields(span,
                                    List.of(createStaticTextComponent("<p style=\"text-align: right; font-weight: bold;\">" + label + "</p>", "p"))));
                            // 输入框列
                            gridColumns.add(createGridColumnWithFields(span,
                                    List.of(createInputComponent(label))));
                        } else {
                            // 如果标签单元格为空，则将两个单元格都作为空列处理，以保持布局
                            gridColumns.add(createGridColumnWithFields(span, Collections.emptyList()));
                            gridColumns.add(createGridColumnWithFields(span, Collections.emptyList()));
                        }
                    }
                }
                if (!gridColumns.isEmpty()) {
                    fields.add(createGridRow(gridColumns));
                }
            } else if (numCells == 1) {
                // 处理合并的单元格，可能是一个标题或一个多行文本域
                String cellText = cells.get(0).getText().trim();
                if (!cellText.isEmpty()) {
                    // 简单判断：如果文本较长，则认为是多行文本，否则是标题
                    if (cellText.length() > 50) {
                        fields.add(createTextareaComponent(cellText));
                    } else {
                        fields.add(createStaticTextComponent("<h3>" + cellText + "</h3>", "h3"));
                    }
                }
            } else {
                log.warn("跳过一个非1、2、4列表格行的解析，列数: {}", numCells);
            }
        }
    }

    /**
     * 创建一个静态文本组件 (StaticText)
     */
    private Map<String, Object> createStaticTextComponent(String content, String tag) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", "statictext_" + UUID.randomUUID().toString().substring(0, 4));
        field.put("type", "StaticText");
        field.put("props", Map.of("content", content, "tag", tag));
        return field;
    }

    /**
     * 创建一个单行输入框组件 (Input)
     */
    private Map<String, Object> createInputComponent(String label) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", "input_" + UUID.randomUUID().toString().substring(0, 4));
        field.put("type", "Input");
        field.put("label", label);
        field.put("props", Map.of("placeholder", "请输入" + label));
        field.put("rules", List.of(Map.of("required", false, "message", "此项为必填项")));
        field.put("visibility", Map.of("enabled", false, "condition", "AND", "rules", Collections.emptyList()));
        return field;
    }

    /**
     * 创建一个多行文本框组件 (Textarea)
     */
    private Map<String, Object> createTextareaComponent(String label) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", "textarea_" + UUID.randomUUID().toString().substring(0, 4));
        field.put("type", "Textarea");
        field.put("label", label);
        field.put("props", Map.of("placeholder", "请输入" + label, "rows", 4));
        field.put("rules", List.of(Map.of("required", false, "message", "此项为必填项")));
        field.put("visibility", Map.of("enabled", false, "condition", "AND", "rules", Collections.emptyList()));
        return field;
    }


    /**
     * 创建一个栅格布局行 (GridRow)
     */
    private Map<String, Object> createGridRow(List<Map<String, Object>> columns) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", "gridrow_" + UUID.randomUUID().toString().substring(0, 4));
        field.put("type", "GridRow");
        field.put("props", Map.of("gutter", 16));
        field.put("columns", columns);
        return field;
    }

    /**
     * 创建一个带有指定内部字段的栅格布局列 (GridCol)
     */
    private Map<String, Object> createGridColumnWithFields(int span, List<Map<String, Object>> fields) {
        Map<String, Object> col = new LinkedHashMap<>();
        col.put("type", "GridCol");
        col.put("props", Map.of("span", span));
        col.put("fields", new ArrayList<>(fields)); // 使用 new ArrayList 确保是可变的
        return col;
    }
}