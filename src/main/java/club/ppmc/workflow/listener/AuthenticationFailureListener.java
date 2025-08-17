package club.ppmc.workflow.listener;

import club.ppmc.workflow.service.LoggingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/**
 * @author cc
 * @description 监听Spring Security认证失败事件
 */
@Component
@RequiredArgsConstructor
public class AuthenticationFailureListener {

    private final LoggingService loggingService;

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String userId = event.getAuthentication().getName();
        String exceptionMessage = event.getException().getMessage();

        String ipAddress = "Unknown";
        String userAgent = "Unknown";

        Object details = event.getAuthentication().getDetails();
        if (details instanceof WebAuthenticationDetails) {
            WebAuthenticationDetails webDetails = (WebAuthenticationDetails) details;
            ipAddress = webDetails.getRemoteAddress();
            // User-Agent is not directly available in WebAuthenticationDetails,
            // You might need to inject HttpServletRequest if you strictly need it.
            // For simplicity, we'll log what we have.
        }

        loggingService.logLoginFailure(userId, ipAddress, userAgent, exceptionMessage);
    }
}