package club.ppmc.workflow.dto;

import lombok.Data;
import java.util.Date; // Camunda 的时间是 java.util.Date

/**
 * @author 你的名字
 * @description 任务的数据传输对象
 */
@Data
public class TaskDto {
    private String camundaTaskId; // 使用 Camunda 的任务 ID
    private String stepName;
    private Date createdAt; // 任务创建时间

    // 关联的业务信息
    private Long formSubmissionId;
    private String formName;
    private String submitterName;
}