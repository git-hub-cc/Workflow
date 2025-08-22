package club.ppmc.workflow.controller;

import club.ppmc.workflow.dto.ReportDto;
import club.ppmc.workflow.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * @author cc
 * @description 【新增】报表相关的 RESTful API 控制器
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    /**
     * API: 根据报表类型Key生成并获取报表数据
     * @param reportKey 报表的唯一标识
     * @return 包含图表配置和数据的报表 DTO
     */
    @GetMapping("/{reportKey}")
    public ResponseEntity<ReportDto> getReport(@PathVariable String reportKey) {
        ReportDto report = reportService.generateReport(reportKey);
        return ResponseEntity.ok(report);
    }
}