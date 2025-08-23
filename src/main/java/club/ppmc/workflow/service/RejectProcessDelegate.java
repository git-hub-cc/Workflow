package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.FormSubmission;
import club.ppmc.workflow.domain.WorkflowInstance;
import club.ppmc.workflow.repository.FormSubmissionRepository;
import club.ppmc.workflow.repository.WorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 这个 Delegate 负责处理流程被拒绝后的归档逻辑。
 */
@Component("rejectProcessDelegate") // 为这个 Bean 命名
@RequiredArgsConstructor
@Slf4j
public class RejectProcessDelegate implements JavaDelegate {

    private final WorkflowInstanceRepository instanceRepository;
    // --- 【核心新增】注入 FormSubmissionRepository ---
    private final FormSubmissionRepository submissionRepository;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) throws Exception {
        String processInstanceId = execution.getProcessInstanceId();
        log.info("执行拒绝归档任务 [RejectProcessDelegate] for process instance: {}", processInstanceId);

        try {
            WorkflowInstance instance = instanceRepository.findByProcessInstanceId(processInstanceId)
                    .orElseThrow(() -> {
                        log.error("在数据库中找不到流程实例ID: {}. 这是一个严重的数据不一致问题。", processInstanceId);
                        return new org.camunda.bpm.engine.delegate.BpmnError("DATA_INCONSISTENCY", "Could not find local workflow instance.");
                    });

            instance.setStatus(WorkflowInstance.Status.REJECTED);
            instance.setCompletedAt(LocalDateTime.now());
            instanceRepository.save(instance);

            // --- 【核心新增】同步更新 FormSubmission 的状态 ---
            FormSubmission submission = instance.getFormSubmission();
            submission.setStatus(FormSubmission.SubmissionStatus.REJECTED);
            submissionRepository.save(submission);

            log.info("流程实例 {} 的状态已成功更新为 REJECTED，关联的申请单 {} 状态也已同步。", instance.getId(), submission.getId());

        } catch (Exception e) {
            log.error("在 RejectProcessDelegate 中执行归档操作时发生异常", e);
            throw e;
        }
    }
}