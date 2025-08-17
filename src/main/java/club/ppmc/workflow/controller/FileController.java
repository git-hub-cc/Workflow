package club.ppmc.workflow.controller;

import club.ppmc.workflow.dto.FileAttachmentDto;
import club.ppmc.workflow.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;

/**
 * @author cc
 * @description 处理文件上传和下载的控制器
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FileController {

    private final FileService fileService;

    /**
     * API: 上传文件
     * @param file 上传的文件
     * @param principal 当前用户信息
     * @return 包含文件元数据的 DTO
     * @throws IOException
     */
    @PostMapping("/upload")
    public ResponseEntity<FileAttachmentDto> uploadFile(@RequestParam("file") MultipartFile file, Principal principal) throws IOException {
        String uploaderId = principal.getName();
        FileAttachmentDto attachmentDto = fileService.storeFile(file, uploaderId);
        return ResponseEntity.ok(attachmentDto);
    }

    /**
     * API: 根据文件ID下载文件
     * @param fileId 文件ID
     * @return 文件资源
     * @throws IOException
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) throws IOException {
        Resource resource = fileService.loadFileAsResource(fileId);
        String originalFilename = fileService.getOriginalFilename(fileId);

        // 对文件名进行编码，防止中文乱码
        String encodedFilename = new String(originalFilename.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                .body(resource);
    }

    /**
     * API: 获取指定表单提交记录的所有附件信息
     * @param submissionId 表单提交记录ID
     * @return 文件元数据 DTO 列表
     */
    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<FileAttachmentDto>> getFilesForSubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(fileService.getFilesBySubmissionId(submissionId));
    }
}