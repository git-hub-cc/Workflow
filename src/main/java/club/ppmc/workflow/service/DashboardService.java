package club.ppmc.workflow.service;

import club.ppmc.workflow.dto.DashboardStatsDto;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstanceQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author cc
 * @description 提供仪表盘数据的服务
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;

    public DashboardStatsDto getStats() {
        DashboardStatsDto stats = new DashboardStatsDto();

        // 1. 正在运行的实例总数
        long runningInstances = runtimeService.createProcessInstanceQuery().active().count();
        stats.setRunningInstances(runningInstances);

        // 获取本月第一天
        Date startOfMonth = Date.from(LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        // 2. 本月已完成的实例
        HistoricProcessInstanceQuery completedQuery = historyService.createHistoricProcessInstanceQuery()
                .finished()
                .startedAfter(startOfMonth);

        long completedThisMonth = completedQuery.count();
        stats.setCompletedThisMonth(completedThisMonth);

        // 3. 计算本月完成实例的状态分布 (通过变量判断)
        // 注意：这种方式可能在大数据量下效率不高，更优方案是使用自定义查询或在流程结束时记录最终状态到独立字段。
        // 这里为了演示，采用变量查询的方式。
        List<String> completedInstanceIds = completedQuery.list().stream()
                .map(pi -> pi.getId())
                .collect(Collectors.toList());

        if (!completedInstanceIds.isEmpty()) {
            long approvedCount = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceIdIn(completedInstanceIds.toArray(new String[0]))
                    .variableName("approved")
                    .variableValueEquals("approved", true)
                    .count();
            stats.setApprovedThisMonth(approvedCount);
            stats.setRejectedThisMonth(completedThisMonth - approvedCount);
        } else {
            stats.setApprovedThisMonth(0L);
            stats.setRejectedThisMonth(0L);
        }

        // 4. 平均处理时长 (示例：仅计算已完成流程的总时长)
        // 更复杂的分析可以针对特定任务节点。
        List<Long> durations = completedQuery.list().stream()
                .map(pi -> pi.getDurationInMillis())
                .collect(Collectors.toList());

        if (!durations.isEmpty()) {
            double avgDuration = durations.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);
            stats.setAvgCompletionTimeMillis(avgDuration);
        } else {
            stats.setAvgCompletionTimeMillis(0.0);
        }

        // 5. 任务瓶颈分析 (示例：找出耗时最长的5个已完成任务)
        // 实际应用中可能需要更复杂的聚合查询
        List<DashboardStatsDto.TaskBottleneckInfo> bottlenecks = historyService.createHistoricTaskInstanceQuery()
                .finished()
                .orderByHistoricTaskInstanceDuration().desc()
                .listPage(0, 5)
                .stream()
                .map(task -> new DashboardStatsDto.TaskBottleneckInfo(
                        task.getName(),
                        task.getProcessDefinitionKey(),
                        task.getDurationInMillis()
                ))
                .collect(Collectors.toList());
        stats.setTaskBottlenecks(bottlenecks);

        return stats;
    }
}