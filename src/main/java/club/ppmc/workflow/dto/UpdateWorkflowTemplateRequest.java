package club.ppmc.workflow.dto;

import lombok.Data;

/**
 * @author cc
 * @description 更新工作流模板（仅保存，不部署）的请求 DTO
 */
@Data
public class UpdateWorkflowTemplateRequest {
    private String bpmnXml;
    private String processDefinitionKey;
}