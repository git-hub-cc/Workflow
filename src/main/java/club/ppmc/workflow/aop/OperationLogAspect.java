package club.ppmc.workflow.aop;

import club.ppmc.workflow.domain.User;
import club.ppmc.workflow.service.LoggingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author cc
 * @description 操作日志切面
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class OperationLogAspect {

    private final LoggingService loggingService;
    private final ObjectMapper objectMapper;
    private final SpelExpressionParser spelExpressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 定义切点，拦截所有被 @LogOperation 注解的方法
     */
    @Pointcut("@annotation(club.ppmc.workflow.aop.LogOperation)")
    public void logOperationPointcut() {
    }

    /**
     * 方法成功返回后执行
     */
    @AfterReturning(pointcut = "logOperationPointcut()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        try {
            // 从安全上下文中获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("无法记录操作日志：未找到认证信息。");
                return;
            }
            Object principal = authentication.getPrincipal();
            String operatorId = "system";
            String operatorName = "系统";
            if (principal instanceof User) {
                User user = (User) principal;
                operatorId = user.getId();
                operatorName = user.getName();
            } else if (principal instanceof String) {
                operatorId = (String) principal;
                // 在某些情况下可能无法立即获取到姓名，可以留空或后续填充
                operatorName = operatorId;
            }

            // 从注解中获取模块和操作信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            LogOperation logAnnotation = method.getAnnotation(LogOperation.class);
            String module = logAnnotation.module();
            String action = logAnnotation.action();

            // 解析SpEL表达式获取TargetId
            String targetId = parseTargetId(logAnnotation.targetIdExpression(), method, joinPoint.getArgs());

            // 获取请求参数作为详情
            String details = getRequestDetails(signature.getParameterNames(), joinPoint.getArgs());

            // 获取IP地址
            String ipAddress = getIpAddress();

            loggingService.logOperation(operatorId, operatorName, module, action, targetId, details, ipAddress);
        } catch (Exception e) {
            log.error("记录操作日志时发生异常", e);
        }
    }

    /**
     * 解析SpEL表达式获取操作对象ID
     */
    private String parseTargetId(String expressionStr, Method method, Object[] args) {
        if (expressionStr == null || expressionStr.isEmpty()) {
            return null;
        }
        try {
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(null, method, args, parameterNameDiscoverer);
            Expression expression = spelExpressionParser.parseExpression(expressionStr);
            Object value = expression.getValue(context, Object.class);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("解析SpEL表达式 '{}' 失败", expressionStr, e);
            return null;
        }
    }

    /**
     * 获取请求参数，序列化为JSON
     */
    private String getRequestDetails(String[] paramNames, Object[] args) {
        if (paramNames == null || args == null || paramNames.length == 0) {
            return "{}";
        }
        Map<String, Object> params = new HashMap<>();
        for (int i = 0; i < paramNames.length; i++) {
            // 过滤掉敏感信息，例如密码
            if (paramNames[i].toLowerCase().contains("password")) {
                params.put(paramNames[i], "******");
            } else {
                params.put(paramNames[i], args[i]);
            }
        }
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            log.error("序列化请求参数失败", e);
            return "{\"error\":\"Serialization failed\"}";
        }
    }

    /**
     * 获取请求IP地址
     */
    private String getIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "Unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}