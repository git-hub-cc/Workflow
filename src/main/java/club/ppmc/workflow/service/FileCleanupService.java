package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.FileAttachment;
import club.ppmc.workflow.repository.FileAttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author cc
 * @description 【新增】定时清理服务，用于处理孤儿文件
 */
@Service
@Slf4j
// --- 【修复】移除此注解，因为它与下面的手动构造函数冲突 ---
// @RequiredArgsConstructor
public class FileCleanupService {

    // --- 【修复】将字段声明为 final，因为它们在构造函数中被初始化且之后不会改变，这是良好的实践 ---
    private final FileAttachmentRepository fileAttachmentRepository;
    private final Path fileStorageLocation;

    // --- 【修复】保留这个唯一的、明确的构造函数，Spring 将使用它来注入依赖 ---
    public FileCleanupService(FileAttachmentRepository fileAttachmentRepository,
                              @Value("${app.file.storage-path}") String storagePath) {
        this.fileAttachmentRepository = fileAttachmentRepository;
        this.fileStorageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    /**
     * 定时任务，每天凌晨3点执行。
     * 清理超过24小时仍处于 TEMPORARY 状态的文件记录及其物理文件。
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupTemporaryFiles() {
        log.info("开始执行临时文件清理任务...");

        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        List<FileAttachment> oldTempFiles = fileAttachmentRepository.findByStatusAndUploadedAtBefore(
                FileAttachment.FileStatus.TEMPORARY,
                cutoffTime
        );

        if (oldTempFiles.isEmpty()) {
            log.info("没有找到需要清理的临时文件。任务结束。");
            return;
        }

        log.info("发现 {} 个需要清理的临时文件记录。", oldTempFiles.size());

        int deletedCount = 0;
        for (FileAttachment attachment : oldTempFiles) {
            try {
                Path filePath = this.fileStorageLocation.resolve(attachment.getStoredFilename()).normalize();
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.debug("成功删除物理文件: {}", filePath);
                    deletedCount++;
                } else {
                    log.warn("物理文件不存在，但将清理其数据库记录: {}", filePath);
                }
            } catch (IOException e) {
                log.error("删除物理文件失败: {}. 将跳过此文件的数据库记录清理。", attachment.getStoredFilename(), e);
                // 如果物理文件删除失败，我们选择不删除数据库记录，以便于后续手动排查
                continue;
            }
        }

        // 筛选出物理文件删除成功（或不存在）的记录进行数据库删除
        List<FileAttachment> recordsToDelete = oldTempFiles.stream()
                .filter(attachment -> {
                    try {
                        Path filePath = this.fileStorageLocation.resolve(attachment.getStoredFilename()).normalize();
                        return !Files.exists(filePath);
                    } catch (Exception e) {
                        // 假设路径有问题，也算作可删除（避免日志中已报告的错误再次阻塞）
                        return true;
                    }
                }).toList();

        if (!recordsToDelete.isEmpty()) {
            fileAttachmentRepository.deleteAll(recordsToDelete);
        }

        log.info("临时文件清理任务完成。共清理了 {} 个物理文件和 {} 条数据库记录。", deletedCount, recordsToDelete.size());
    }
}