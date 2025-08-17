package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.User;
import club.ppmc.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Optional;

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

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // 1. 从流程变量中获取发起人ID
        String submitterId = (String) execution.getVariable("submitterId");
        if (submitterId == null) {
            log.error("流程变量 'submitterId' 未设置，无法查找审批人。流程实例ID: {}", execution.getProcessInstanceId());
            throw new BpmnError("NO_SUBMITTER", "发起人ID未提供");
        }

        // 2. 从流程变量中获取要查找的上级级别 (例如，1 表示直接上级, 2 表示上级的上级)
        // 这个变量可以由BPMN的用户任务扩展属性或输入参数定义
        Integer managerLevel = getIntegerVariable(execution, "managerLevel", 1);

        log.info("开始查找审批人：发起人 [{}], 查找级别 [{}]", submitterId, managerLevel);

        // 3. 查找发起人实体
        User currentUser = userRepository.findById(submitterId)
                .orElseThrow(() -> {
                    log.error("数据库中未找到ID为 '{}' 的用户", submitterId);
                    return new BpmnError("USER_NOT_FOUND", "发起人用户不存在");
                });

        // 4. 循环查找N级上级
        User manager = currentUser;
        for (int i = 0; i < managerLevel; i++) {
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

    /**
     * 安全地从流程变量中获取一个整数值，如果不存在或类型不匹配，则返回默认值。
     */
    private Integer getIntegerVariable(DelegateExecution execution, String variableName, Integer defaultValue) {
        Object value = execution.getVariable(variableName);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}