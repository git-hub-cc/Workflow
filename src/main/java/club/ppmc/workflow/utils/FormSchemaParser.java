package club.ppmc.workflow.utils;

import club.ppmc.workflow.dto.FormFieldConfigDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cc
 * @description 【新增】一个用于解析表单定义JSON schema的工具类
 */
public class FormSchemaParser {

    /**
     * 解析表单schema JSON字符串，并提取出可筛选和可列表展示的字段。
     *
     * @param schemaJson   表单定义的JSON字符串
     * @param objectMapper Jackson的ObjectMapper实例
     * @return 包含两个字段列表的ParsedSchema对象
     * @throws IOException 如果JSON解析失败
     */
    public static ParsedSchema parse(String schemaJson, ObjectMapper objectMapper) throws IOException {
        List<FormFieldConfigDto> filterableFields = new ArrayList<>();
        List<FormFieldConfigDto> listDisplayFields = new ArrayList<>();

        JsonNode rootNode = objectMapper.readTree(schemaJson);
        JsonNode fieldsNode = rootNode.path("fields");

        if (fieldsNode.isArray()) {
            flattenAndProcessFields(fieldsNode, filterableFields, listDisplayFields);
        }

        return new ParsedSchema(filterableFields, listDisplayFields);
    }

    /**
     * 递归地扁平化字段结构（处理GridRow, Collapse等布局组件）并处理字段。
     */
    private static void flattenAndProcessFields(JsonNode fieldsNode, List<FormFieldConfigDto> filterable, List<FormFieldConfigDto> listDisplay) {
        for (JsonNode fieldNode : fieldsNode) {
            String type = fieldNode.path("type").asText();

            // 1. 处理可交互字段
            if (fieldNode.has("id") && !type.startsWith("Grid") && !type.equals("Collapse") && !type.equals("StaticText")) {
                String id = fieldNode.path("id").asText();
                String label = fieldNode.path("label").asText(id); // 如果没有label，使用id作为备用

                if (fieldNode.path("isFilterable").asBoolean(false)) {
                    filterable.add(new FormFieldConfigDto(id, label, type));
                }
                if (fieldNode.path("showInList").asBoolean(false)) {
                    listDisplay.add(new FormFieldConfigDto(id, label, type));
                }
            }

            // 2. 递归处理布局容器内的字段
            if ("GridRow".equals(type) && fieldNode.has("columns")) {
                for (JsonNode columnNode : fieldNode.path("columns")) {
                    if (columnNode.has("fields")) {
                        flattenAndProcessFields(columnNode.path("fields"), filterable, listDisplay);
                    }
                }
            } else if ("Collapse".equals(type) && fieldNode.has("panels")) {
                for (JsonNode panelNode : fieldNode.path("panels")) {
                    if (panelNode.has("fields")) {
                        flattenAndProcessFields(panelNode.path("fields"), filterable, listDisplay);
                    }
                }
            }
        }
    }

    /**
     * 用于封装解析结果的内部静态类。
     */
    @Getter
    @RequiredArgsConstructor
    public static class ParsedSchema {
        private final List<FormFieldConfigDto> filterableFields;
        private final List<FormFieldConfigDto> listDisplayFields;
    }
}