package club.ppmc.workflow.dto;

import lombok.Data;

/**
 * @author 你的名字
 * @description 工作流模板的响应 DTO (用于前端设计器加载)
 */
@Data
public class WorkflowTemplateResponse {
    private Long formDefinitionId;
    private String bpmnXml;
    private String processDefinitionKey;
}