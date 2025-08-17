package club.ppmc.workflow.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * @author 你的名字
 * @description 文件附件的数据传输对象
 */
@Data
@Builder
public class FileAttachmentDto {
    private Long id;
    private String originalFilename;
    private String contentType;
    private Long size;
    private LocalDateTime uploadedAt;
}