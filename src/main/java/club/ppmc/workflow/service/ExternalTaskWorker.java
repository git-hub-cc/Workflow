package club.ppmc.workflow.worker;

import club.ppmc.workflow.domain.WorkflowInstance;
import club.ppmc.workflow.repository.WorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * @author 你的名字
 * @description 外部任务工作者配置。
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ExternalTaskWorker {

    private final WorkflowInstanceRepository instanceRepository;

    /**
     * 定义一个处理"流程归档"任务的 Handler。
     * @ExternalTaskSubscription("archive-process") 订阅名为 "archive-process" 的主题。
     */
    @Bean
    @ExternalTaskSubscription("archive-process")
    public ExternalTaskHandler archiveProcessHandler() {
        return new ExternalTaskHandler() {
            @Override
            @Transactional // 确保数据库操作在事务中进行
            public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
                log.info("接收到 'archive-process' 任务，流程实例ID: {}", externalTask.getProcessInstanceId());

                try {
                    String processInstanceId = externalTask.getProcessInstanceId();
                    Optional<WorkflowInstance> instanceOpt = instanceRepository.findByProcessInstanceId(processInstanceId);

                    if (instanceOpt.isPresent()) {
                        WorkflowInstance instance = instanceOpt.get();
                        instance.setStatus(WorkflowInstance.Status.APPROVED);
                        instance.setCompletedAt(LocalDateTime.now());
                        instanceRepository.save(instance);

                        log.info("流程实例 {} 的状态已成功更新为 APPROVED。", instance.getId());
                        externalTaskService.complete(externalTask);
                    } else {
                        log.error("未能在本地数据库中找到流程实例ID: {}", processInstanceId);
                        externalTaskService.handleFailure(
                                externalTask, "Business Error", "Could not find local process instance.", 0, 0
                        );
                    }
                } catch (Exception e) {
                    log.error("处理 'archive-process' 任务时发生异常", e);
                    externalTaskService.handleFailure(
                            externalTask,
                            "Unhandled Exception",
                            e.getMessage(),
                            (externalTask.getRetries() == null ? 3 : externalTask.getRetries() - 1),
                            5000L
                    );
                }
            }
        };
    }

    // 新增一个处理拒绝归档的 Handler
    @Bean
    @ExternalTaskSubscription("reject-process")
    public ExternalTaskHandler rejectProcessHandler() {
        return (externalTask, externalTaskService) -> {
            log.info("接收到 'reject-process' 任务，流程实例ID: {}", externalTask.getProcessInstanceId());
            try {
                String processInstanceId = externalTask.getProcessInstanceId();
                instanceRepository.findByProcessInstanceId(processInstanceId).ifPresentOrElse(instance -> {
                    instance.setStatus(WorkflowInstance.Status.REJECTED);
                    instance.setCompletedAt(LocalDateTime.now());
                    instanceRepository.save(instance);
                    log.info("流程实例 {} 的状态已成功更新为 REJECTED。", instance.getId());
                    externalTaskService.complete(externalTask);
                }, () -> {
                    log.error("未能在本地数据库中找到流程实例ID: {}", processInstanceId);
                    externalTaskService.handleFailure(externalTask, "Business Error", "Could not find local process instance.", 0, 0);
                });
            } catch (Exception e) {
                log.error("处理 'reject-process' 任务时发生异常", e);
                externalTaskService.handleFailure(externalTask, "Unhandled Exception", e.getMessage(), 0, 0);
            }
        };
    }
}