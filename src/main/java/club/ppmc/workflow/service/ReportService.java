package club.ppmc.workflow.service;

import club.ppmc.workflow.dto.ReportDto;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.HistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author cc
 * @description 【新增】报表服务
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final HistoryService historyService;

    public ReportDto generateReport(String reportKey) {
        switch (reportKey) {
            case "process_completion_stats":
                return generateProcessCompletionStats();
            // 可以根据 reportKey 添加更多报表生成逻辑
            default:
                throw new IllegalArgumentException("未知的报表类型: " + reportKey);
        }
    }

    private ReportDto generateProcessCompletionStats() {
        // 示例：按流程定义统计已完成实例的数量
        List<Map<String, Object>> data = historyService.createHistoricProcessInstanceQuery()
                .finished()
                .list()
                .stream()
                .collect(Collectors.groupingBy(
                        pi -> pi.getProcessDefinitionName() != null ? pi.getProcessDefinitionName() : "未命名流程",
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(entry -> {
                    // 【FIX】使用 HashMap 代替 Map.of() 来解决类型推断问题
                    Map<String, Object> map = new HashMap<>();
                    map.put("processName", entry.getKey());
                    map.put("count", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());

        // ECharts 的 options
        Map<String, Object> options = Map.of(
                "tooltip", Map.of("trigger", "item"),
                "legend", Map.of("top", "5%", "left", "center"),
                "series", List.of(
                        Map.of(
                                "name", "完成实例数",
                                "type", "pie",
                                "radius", "50%",
                                "data", data.stream().map(d -> {
                                    Map<String, Object> map = new HashMap<>();
                                    map.put("name", d.get("processName"));
                                    map.put("value", d.get("count"));
                                    return map;
                                }).collect(Collectors.toList()),
                                "emphasis", Map.of("itemStyle", Map.of("shadowBlur", 10, "shadowOffsetX", 0, "shadowColor", "rgba(0, 0, 0, 0.5)"))
                        )
                )
        );

        return ReportDto.builder()
                .title("各流程完成实例数分布")
                .type("pie")
                .options(options)
                .build();
    }
}