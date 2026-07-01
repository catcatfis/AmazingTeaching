package com.cupk.amazingteaching.common.aspect;

import com.cupk.amazingteaching.common.annotation.LogOperation;
import com.cupk.amazingteaching.module.log.entity.SysLog;
import com.cupk.amazingteaching.module.log.mapper.SysLogMapper;
import com.cupk.amazingteaching.common.security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final SysLogMapper sysLogMapper;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    @Around("@annotation(logOperation)")
    public Object around(ProceedingJoinPoint joinPoint, LogOperation logOperation) throws Throwable {
        long startTime = System.currentTimeMillis();
        SysLog sysLog = new SysLog();
        sysLog.setModule(logOperation.module());
        sysLog.setOperation(logOperation.operation());
        sysLog.setDescription(logOperation.description());

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            sysLog.setRequestUrl(request.getRequestURI());
            sysLog.setRequestMethod(request.getMethod());
            sysLog.setRequestIp(getIpAddress(request));

            // 获取当前用户
            String token = getTokenFromRequest(request);
            if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
                sysLog.setUsername(jwtUtils.getUsernameFromToken(token));
                sysLog.setUserId(jwtUtils.getUserIdFromToken(token));
            }
        }

        // 方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        sysLog.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());

        // 请求参数
        try {
            sysLog.setRequestParams(objectMapper.writeValueAsString(joinPoint.getArgs()));
        } catch (Exception e) {
            sysLog.setRequestParams("参数序列化失败");
        }

        Object result;
        try {
            result = joinPoint.proceed();
            sysLog.setResult(1);
        } catch (Throwable e) {
            sysLog.setResult(0);
            sysLog.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            sysLog.setCostTime(System.currentTimeMillis() - startTime);
            try {
                sysLogMapper.insert(sysLog);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }

        return result;
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
