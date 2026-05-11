package org.example.expert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j(topic = "AdminAccessLog")
@Aspect
@Component
@RequiredArgsConstructor
public class AdminAccessLoggingAspect {

    private final ObjectMapper objectMapper;

    // 1. 타겟 메서드 지정 (어드민 전용 2개 메서드 경로를 명시)
    @Pointcut("execution(* org.example.expert.domain.comment.controller.CommentAdminController.deleteComment(..)) || " +
            "execution(* org.example.expert.domain.user.controller.UserAdminController.changeUserRole(..))")
    public void adminMethods() {}

    // 2. 메서드 실행 전후(@Around)로 로깅 수행
    @Around("adminMethods()")
    public Object logAdminAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        // 현재 들어온 HTTP 요청 객체 가져오기
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        // 요구사항 정보 추출
        Long userId = (Long) request.getAttribute("userId"); // JwtFilter에서 request에 담아둔 값
        String requestUrl = request.getRequestURI();
        LocalDateTime requestTime = LocalDateTime.now();

        // Request Body 추출 (파라미터 중에서 Request DTO 객체 찾기)
        Object requestBody = null;
        for (Object arg : joinPoint.getArgs()) {
            if (arg != null && arg.getClass().getSimpleName().endsWith("Request")) {
                requestBody = arg;
                break;
            }
        }

        // [기준점] 실제 컨트롤러 메서드 실행
        Object result = joinPoint.proceed();

        // Response Body 추출 및 JSON 변환
        String requestBodyJson = requestBody != null ? objectMapper.writeValueAsString(requestBody) : "null or empty";
        String responseBodyJson = result != null ? objectMapper.writeValueAsString(result) : "null or void";

        // 로그 출력 (Console에 기록됨)
        log.info("::: Admin API Access Log :::");
        log.info("Request User ID : {}", userId);
        log.info("Request Time    : {}", requestTime);
        log.info("Request URL     : {}", requestUrl);
        log.info("Request Body    : {}", requestBodyJson);
        log.info("Response Body   : {}", responseBodyJson);
        log.info("::::::::::::::::::::::::::::");

        return result;
    }
}