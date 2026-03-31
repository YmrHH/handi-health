package com.example.zhinengsuifang.service;

import com.example.zhinengsuifang.entity.OperationAuditLog;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.util.AuditUtil;
import com.example.zhinengsuifang.util.AuthHeaderUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.time.LocalDateTime;
import java.util.Map;

@Aspect
@Component
public class OperationAuditLogAspect {

    @Resource
    private OperationAuditLogService operationAuditLogService;

    @Resource
    private UserMapper userMapper;

    @Around("within(com.example.zhinengsuifang.contoller..*) && (@within(org.springframework.web.bind.annotation.RestController) || @annotation(org.springframework.web.bind.annotation.RequestMapping) || @annotation(org.springframework.web.bind.annotation.GetMapping) || @annotation(org.springframework.web.bind.annotation.PostMapping) || @annotation(org.springframework.web.bind.annotation.PutMapping) || @annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        long startAt = System.currentTimeMillis();

        HttpServletRequest request = null;
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                request = attrs.getRequest();
            }
        } catch (Exception ignored) {
        }

        Long operatorId = AuthHeaderUtil.getUserId(request);
        String operatorName = null;
        if (operatorId != null) {
            User u = userMapper.findById(operatorId);
            if (u != null) {
                operatorName = u.getName() != null && !u.getName().trim().isEmpty() ? u.getName().trim() : u.getUsername();
            }
        }

        String uri = request == null ? null : request.getRequestURI();
        String method = request == null ? null : request.getMethod();
        String query = request == null ? null : request.getQueryString();

        if (uri != null && uri.contains("/api/system/data-log")) {
            return pjp.proceed();
        }

        String module = AuditUtil.guessModule(uri);
        String action = AuditUtil.describeBusinessAction(method, uri);

        String body = null;
        if (request instanceof ContentCachingRequestWrapper) {
            ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf != null && buf.length > 0) {
                try {
                    body = new String(buf, java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception ignored) {
                }
            }
        }

        String target = AuditUtil.extractTarget(uri, query, body);

        OperationAuditLog log = new OperationAuditLog();
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setModule(module);
        log.setAction(action);
        log.setTarget(target);
        log.setRequestMethod(method);
        log.setRequestUri(uri);
        log.setRequestQuery(AuditUtil.safeTruncate(query, 1024));
        log.setRequestBody(AuditUtil.safeTruncate(body, 2000));
        log.setCreatedAt(LocalDateTime.now());

        try {
            Object ret = pjp.proceed();

            boolean success = true;
            if (ret instanceof Map) {
                Object s = ((Map<?, ?>) ret).get("success");
                if (s instanceof Boolean) {
                    success = (Boolean) s;
                }
            }
            log.setSuccess(success ? 1 : 0);
            log.setDurationMs(System.currentTimeMillis() - startAt);
            log.setErrorMessage(null);
            operationAuditLogService.record(log);
            return ret;
        } catch (Throwable ex) {
            log.setSuccess(0);
            log.setDurationMs(System.currentTimeMillis() - startAt);
            log.setErrorMessage(AuditUtil.safeTruncate(ex.getMessage(), 512));
            try {
                operationAuditLogService.record(log);
            } catch (Exception ignored) {
            }
            throw ex;
        }
    }
}
