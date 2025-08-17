package club.ppmc.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @author cc
 * @description 仪表盘统计数据的 DTO
 */
@Data
public class DashboardStatsDto {
    private long runningInstances;
    private long completedThisMonth;
    private long approvedThisMonth;
    private long rejectedThisMonth;
    private double avgCompletionTimeMillis;
    private List<TaskBottleneckInfo> taskBottlenecks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskBottleneckInfo {
        private String taskName;
        private String processDefinitionKey;
        private Long durationMillis;
    }
}