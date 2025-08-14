package club.ppmc.workflow.controller;

import club.ppmc.workflow.domain.User;
import club.ppmc.workflow.dto.DeployWorkflowRequest;
import club.ppmc.workflow.dto.UserDto;
import club.ppmc.workflow.dto.WorkflowTemplateResponse;
import club.ppmc.workflow.repository.UserRepository;
import club.ppmc.workflow.repository.WorkflowTemplateRepository;
import club.ppmc.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 你的名字
 * @description 工作流相关的 RESTful API 控制器
 */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final UserRepository userRepository;
    private final WorkflowTemplateRepository templateRepository;

    /**
     * API: 部署一个新的工作流模板
     * METHOD: POST
     * PATH: /api/workflows/deploy
     */
    @PostMapping("/deploy")
    public ResponseEntity<Void> deployWorkflow(@RequestBody DeployWorkflowRequest request) {
        workflowService.deployWorkflow(request);
        return ResponseEntity.ok().build();
    }

    /**
     * API: 根据表单ID获取其工作流模板
     * METHOD: GET
     * PATH: /api/workflows/templates?formId={formId}
     */
    @GetMapping("/templates")
    public ResponseEntity<WorkflowTemplateResponse> getTemplateByFormId(@RequestParam Long formId) {
        return templateRepository.findByFormDefinitionId(formId)
                .map(template -> {
                    WorkflowTemplateResponse dto = new WorkflowTemplateResponse();
                    dto.setFormDefinitionId(template.getFormDefinition().getId());
                    dto.setBpmnXml(template.getBpmnXml());
                    dto.setProcessDefinitionKey(template.getProcessDefinitionKey());
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * API: 获取所有用户列表 (用于在前端下拉选择审批人)
     * METHOD: GET
     * PATH: /api/workflows/users
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserDto> userDtos = users.stream().map(user -> {
            UserDto dto = new UserDto();
            dto.setId(user.getId());
            dto.setName(user.getName());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }
}