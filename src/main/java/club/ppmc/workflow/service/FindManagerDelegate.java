package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.User;
import club.ppmc.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * @author cc
 * @description 一个 Camunda JavaDelegate，用于动态查找审批人。
 * 它可以在BPMN流程中作为一个服务任务被调用。
 * 支持查找N级上级。
 */
@Component("findManagerDelegate")
@RequiredArgsConstructor
@Slf4j
public class FindManagerDelegate implements JavaDelegate {

    private final UserRepository userRepository;

    /**
     * 【核心修复】添加此字段以接收来自BPMN的字段注入。
     * Camunda引擎会自动将BPMN中配置的 <camunda:field name="managerLevel"> 的值注入到这里。
     */
    private Expression managerLevel;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // 1. 从流程变量中获取发起人ID
        String submitterId = (String) execution.getVariable("submitterId");
        if (submitterId == null) {
            log.error("流程变量 'submitterId' 未设置，无法查找审批人。流程实例ID: {}", execution.getProcessInstanceId());
            throw new BpmnError("NO_SUBMITTER", "发起人ID未提供");
        }

        // 2. 【核心修复】从注入的字段中计算上级级别，而不是从流程变量获取
        int level = 1; // 默认查找一级上级
        if (managerLevel != null) {
            Object value = managerLevel.getValue(execution);
            if (value != null) {
                try {
                    level = Integer.parseInt(value.toString());
                } catch (NumberFormatException e) {
                    log.warn("无法将 'managerLevel' 字段值 '{}' 解析为整数。将使用默认值 1。", value);
                }
            }
        }

        log.info("开始查找审批人：发起人 [{}], 查找级别 [{}]", submitterId, level);

        // 3. 查找发起人实体
        User currentUser = userRepository.findById(submitterId)
                .orElseThrow(() -> {
                    log.error("数据库中未找到ID为 '{}' 的用户", submitterId);
                    return new BpmnError("USER_NOT_FOUND", "发起人用户不存在");
                });

        // 4. 循环查找N级上级
        User manager = currentUser;
        for (int i = 0; i < level; i++) {
            manager = manager.getManager();
            if (manager == null) {
                log.error("用户 '{}' 的第 {} 级上级不存在 (查找链中断)", currentUser.getId(), i + 1);
                throw new BpmnError("MANAGER_NOT_FOUND", "无法找到指定级别的上级审批人");
            }
        }

        String assigneeId = manager.getId();
        log.info("成功找到审批人: [{}] ({})", manager.getName(), assigneeId);

        // 5. 将找到的审批人ID设置到一个新的流程变量中 (例如 'assignee')
        // 后续的用户任务就可以使用 ${assignee} 来动态分配任务
        execution.setVariable("assignee", assigneeId);
    }
}