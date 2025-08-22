package club.ppmc.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cc
 * @description 【新增】用于返回BPMN XML内容的 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BpmnXmlDto {

    /**
     * BPMN 2.0 流程定义的 XML 字符串
     */
    private String bpmnXml;
}