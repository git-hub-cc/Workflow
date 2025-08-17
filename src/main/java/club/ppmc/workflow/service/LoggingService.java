package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.LoginLog;
import club.ppmc.workflow.domain.OperationLog;
import club.ppmc.workflow.repository.LoginLogRepository;
import club.ppmc.workflow.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author cc
 * @description 封装日志记录的业务逻辑服务
 */
@Service
@RequiredArgsConstructor
public class LoggingService {

    private final LoginLogRepository loginLogRepository;
    private final OperationLogRepository operationLogRepository;

    /**
     * 记录登录成功日志
     */
    @Async // 异步执行，不影响主流程性能
    @Transactional
    public void logLoginSuccess(String userId, String ipAddress, String userAgent) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setStatus(LoginLog.LoginStatus.SUCCESS);
        loginLogRepository.save(log);
    }

    /**
     * 记录登录失败日志
     */
    @Async
    @Transactional
    public void logLoginFailure(String userId, String ipAddress, String userAgent, String reason) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setStatus(LoginLog.LoginStatus.FAILURE);
        log.setFailureReason(reason);
        loginLogRepository.save(log);
    }

    /**
     * 记录操作日志
     */
    @Async
    @Transactional
    public void logOperation(String operatorId, String operatorName, String module, String action, String targetId, String details, String ipAddress) {
        OperationLog log = new OperationLog();
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setModule(module);
        log.setAction(action);
        log.setTargetId(targetId);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        operationLogRepository.save(log);
    }
}