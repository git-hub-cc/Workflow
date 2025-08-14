package club.ppmc.workflow.dto;

import lombok.Data;

/**
 * @author 你的名字
 * @description 部署工作流的请求 DTO
 */
@Data
public class DeployWorkflowRequest {
    private Long formDefinitionId;
    private String bpmnXml;
    private String processDefinitionKey;
}