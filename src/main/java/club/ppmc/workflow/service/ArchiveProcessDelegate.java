package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.FormSubmission;
import club.ppmc.workflow.domain.WorkflowInstance;
import club.ppmc.workflow.integration.erp.ErpService;
import club.ppmc.workflow.integration.erp.dto.InventoryDeductionRequest;
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
 * 一个 Spring Bean 实现 Camunda 的 JavaDelegate 接口。
 * 当流程执行到关联的服务任务时，execute 方法将被调用。
 * 这个类负责处理流程审批通过后的归档逻辑。
 */
@Component("archiveProcessDelegate") // 为这个 Bean 命名，以便在 BPMN 中引用
@RequiredArgsConstructor
@Slf4j
public class ArchiveProcessDelegate implements JavaDelegate {

    private final WorkflowInstanceRepository instanceRepository;
    private final ErpService erpService;
    // --- 【核心新增】注入 FormSubmissionRepository ---
    private final FormSubmissionRepository submissionRepository;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) throws Exception {
        String processInstanceId = execution.getProcessInstanceId();
        log.info("执行归档任务 [ArchiveProcessDelegate] for process instance: {}", processInstanceId);

        try {
            WorkflowInstance instance = instanceRepository.findByProcessInstanceId(processInstanceId)
                    .orElseThrow(() -> {
                        log.error("在数据库中找不到流程实例ID: {}. 这是一个严重的数据不一致问题。", processInstanceId);
                        // 抛出 BPMN 错误，可以在流程中捕获它
                        return new org.camunda.bpm.engine.delegate.BpmnError("DATA_INCONSISTENCY", "Could not find local workflow instance.");
                    });

            instance.setStatus(WorkflowInstance.Status.APPROVED);
            instance.setCompletedAt(LocalDateTime.now());
            instanceRepository.save(instance);

            // --- 【核心新增】同步更新 FormSubmission 的状态 ---
            FormSubmission submission = instance.getFormSubmission();
            submission.setStatus(FormSubmission.SubmissionStatus.APPROVED);
            submissionRepository.save(submission);

            log.info("流程实例 {} 的状态已成功更新为 APPROVED，关联的申请单 {} 状态也已同步。", instance.getId(), submission.getId());


            // --- 【核心修改】从流程变量中动态获取数据并调用ERP服务 ---
            // 假设表单中有一个ID为 "materialSku" 的字段和一个ID为 "quantity" 的字段
            String skuFromProcess = (String) execution.getVariable("materialSku");
            Object quantityObj = execution.getVariable("quantity");

            if (skuFromProcess != null && quantityObj instanceof Number) {
                int quantityToDeduct = ((Number) quantityObj).intValue();
                log.info("准备调用ERP接口扣减库存: SKU = {}, 数量 = {}", skuFromProcess, quantityToDeduct);
                erpService.deductInventory(new InventoryDeductionRequest(skuFromProcess, quantityToDeduct));
            } else {
                log.warn("流程实例 {} 缺少 'materialSku' 或 'quantity' 变量，跳过ERP库存扣减。", processInstanceId);
            }
            // --- 【修改结束】 ---


        } catch (Exception e) {
            log.error("在 ArchiveProcessDelegate 中执行归档操作时发生异常", e);
            // 将原始异常重新抛出，Camunda 引擎的事务会回滚
            throw e;
        }
    }
}