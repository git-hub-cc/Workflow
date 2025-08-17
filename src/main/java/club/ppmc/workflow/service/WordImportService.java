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

/**
 * @author cc
 * @description 负责从 Word(.docx) 文档解析并生成表单定义的服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WordImportService {

    private final ObjectMapper objectMapper;

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
        // --- 【核心修正】 ---
        // 首先，确保表格有行，否则无法判断列数
        if (table.getRows() == null || table.getRows().isEmpty()) {
            log.warn("跳过一个没有行的空表格。");
            return;
        }

        // 通过第一行的单元格数量来获取列数
        int numCols = table.getRow(0).getTableCells().size();

        // MVP 阶段：只处理常见的两列表单
        if (numCols != 2) {
            log.warn("跳过一个非2列表格的解析，列数: {}", numCols);
            return;
        }
        // --- 【修正结束】 ---

        List<Map<String, Object>> columns = new ArrayList<>();
        // 左列
        columns.add(createGridColumn(12));
        // 右列
        columns.add(createGridColumn(12));

        for (XWPFTableRow row : table.getRows()) {
            List<XWPFTableCell> cells = row.getTableCells();
            // 再次确认当前行是否也是2列，以应对合并单元格等复杂情况
            if (cells.size() == 2) {
                String label = cells.get(0).getText().trim().replaceAll("[:：\\s]+$", ""); // 清理标签文本

                if (!label.isEmpty()) {
                    // 左侧单元格作为标签，放入左侧布局列
                    ((List<Map<String, Object>>) columns.get(0).get("fields"))
                            .add(createStaticTextComponent("<p style=\"text-align: right; font-weight: bold;\">" + label + "</p>", "p"));

                    // 右侧单元格作为输入框，放入右侧布局列
                    ((List<Map<String, Object>>) columns.get(1).get("fields"))
                            .add(createInputComponent(label));
                }
            }
        }

        // 如果表格解析出了有效的组件，则将其作为一个 GridRow 添加
        if (!((List<Map<String,Object>>) columns.get(0).get("fields")).isEmpty()) {
            fields.add(createGridRow(columns));
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
        // 默认校验规则
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
     * 创建一个栅格布局列 (GridCol)
     */
    private Map<String, Object> createGridColumn(int span) {
        Map<String, Object> col = new LinkedHashMap<>();
        col.put("type", "GridCol");
        col.put("props", Map.of("span", span));
        col.put("fields", new ArrayList<>());
        return col;
    }
}