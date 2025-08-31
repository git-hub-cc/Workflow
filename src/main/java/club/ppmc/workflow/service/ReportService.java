package club.ppmc.workflow.service;

import club.ppmc.workflow.dto.ReportDto;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.HistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

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
        return switch (reportKey) {
            case "process_completion_stats" -> generateProcessCompletionStats();
            // 【新增】新增报表 case
            case "monthly_submission_trend" -> generateMonthlySubmissionTrend();
            default -> throw new IllegalArgumentException("未知的报表类型: " + reportKey);
        };
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

    /**
     * 【新增】生成近12个月的流程提交趋势报表
     */
    private ReportDto generateMonthlySubmissionTrend() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.withDayOfMonth(1).minusMonths(11);

        // 获取时间范围内的所有已启动流程
        Map<String, Long> monthlyCounts = historyService.createHistoricProcessInstanceQuery()
                .startedAfter(Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .startedBefore(Date.from(endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .list()
                .stream()
                .collect(Collectors.groupingBy(
                        pi -> {
                            LocalDate startedDate = pi.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                            return String.format("%d-%02d", startedDate.getYear(), startedDate.getMonthValue());
                        },
                        Collectors.counting()
                ));

        // 生成X轴标签 (近12个月)
        List<String> months = LongStream.range(0, 12)
                .mapToObj(startDate::plusMonths)
                .map(date -> String.format("%d-%02d", date.getYear(), date.getMonthValue()))
                .collect(Collectors.toList());

        // 填充数据
        List<Long> data = months.stream()
                .map(month -> monthlyCounts.getOrDefault(month, 0L))
                .collect(Collectors.toList());

        // ECharts options
        Map<String, Object> options = Map.of(
                "tooltip", Map.of("trigger", "axis"),
                "grid", Map.of("left", "3%", "right", "4%", "bottom", "3%", "containLabel", true),
                "xAxis", Map.of("type", "category", "boundaryGap", false, "data", months),
                "yAxis", Map.of("type", "value", "name", "提交量"),
                "series", List.of(
                        Map.of(
                                "name", "月提交量",
                                "type", "line",
                                "stack", "Total",
                                "areaStyle", Map.of(),
                                "emphasis", Map.of("focus", "series"),
                                "data", data
                        )
                )
        );

        return ReportDto.builder()
                .title("近12个月流程提交趋势")
                .type("line")
                .options(options)
                .build();
    }
}