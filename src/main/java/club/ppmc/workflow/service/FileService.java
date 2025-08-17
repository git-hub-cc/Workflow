package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.FileAttachment;
import club.ppmc.workflow.dto.FileAttachmentDto;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.repository.FileAttachmentRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class FileService {

    private final Path fileStorageLocation;
    private final FileAttachmentRepository fileAttachmentRepository;

    public FileService(@Value("${app.file.storage-path}") String storagePath,
                       FileAttachmentRepository fileAttachmentRepository) {
        this.fileStorageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        this.fileAttachmentRepository = fileAttachmentRepository;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("无法创建用于存储上传文件的目录！", ex);
        }
    }

    public FileAttachmentDto storeFile(MultipartFile file, String uploaderId) throws IOException {
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String storedFilename = UUID.randomUUID().toString() + (extension != null ? "." + extension : "");

        Path targetLocation = this.fileStorageLocation.resolve(storedFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        FileAttachment attachment = new FileAttachment();
        attachment.setOriginalFilename(originalFilename);
        attachment.setStoredFilename(storedFilename);
        attachment.setContentType(file.getContentType());
        attachment.setSize(file.getSize());
        attachment.setUploaderId(uploaderId);

        FileAttachment savedAttachment = fileAttachmentRepository.save(attachment);
        return convertToDto(savedAttachment);
    }

    public Resource loadFileAsResource(Long fileId) throws MalformedURLException {
        FileAttachment attachment = fileAttachmentRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("文件未找到 ID: " + fileId));

        Path filePath = this.fileStorageLocation.resolve(attachment.getStoredFilename()).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists()) {
            return resource;
        } else {
            throw new ResourceNotFoundException("文件未找到: " + attachment.getOriginalFilename());
        }
    }

    public String getOriginalFilename(Long fileId) {
        return fileAttachmentRepository.findById(fileId)
                .map(FileAttachment::getOriginalFilename)
                .orElseThrow(() -> new ResourceNotFoundException("文件未找到 ID: " + fileId));
    }

    @Transactional(readOnly = true)
    public List<FileAttachmentDto> getFilesBySubmissionId(Long submissionId) {
        return fileAttachmentRepository.findByFormSubmissionId(submissionId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private FileAttachmentDto convertToDto(FileAttachment entity) {
        return FileAttachmentDto.builder()
                .id(entity.getId())
                .originalFilename(entity.getOriginalFilename())
                .contentType(entity.getContentType())
                .size(entity.getSize())
                .uploadedAt(entity.getUploadedAt())
                .build();
    }
}