package club.ppmc.workflow.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author cc
 * @description 【新增】通用报表数据传输对象
 */
@Data
@Builder
public class ReportDto {
    private String title;
    private String type; // e.g., "bar", "pie", "line", "table"
    private Map<String, Object> options; // For ECharts options
    private List<Map<String, Object>> tableData;
    private List<ColumnDef> tableColumns;

    @Data
    @Builder
    public static class ColumnDef {
        private String key;
        private String title;
    }
}