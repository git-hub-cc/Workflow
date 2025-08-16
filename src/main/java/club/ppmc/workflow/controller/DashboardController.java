package club.ppmc.workflow.controller;

import club.ppmc.workflow.dto.DashboardStatsDto;
import club.ppmc.workflow.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 你的名字
 * @description 仪表盘数据相关的控制器
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')") // 仅限管理员访问
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * API: 获取仪表盘的统计数据
     * @return 包含各种统计指标的 DTO
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }
}