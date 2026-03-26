package com.geollm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geollm.dto.ApiResponse;
import com.geollm.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class JwtAuthInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;
    private final ObjectMapper om = new ObjectMapper();

    public JwtAuthInterceptor(String jwtKey) {
        this.jwtUtil = new JwtUtil(jwtKey);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        String path = request.getRequestURI();

        // 放行预检请求
        if ("OPTIONS".equalsIgnoreCase(method)) return true;
        // 放行登录/注册/改密
        if (path.startsWith("/user/") || "/user".equals(path)) return true;

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeAuthError(response, 320);
            return false;
        }

        String token = auth.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            writeAuthError(response, 320);
            return false;
        }

        try {
            Claims claims = jwtUtil.parse(token);
            String username = claims.get("username", String.class);
            Integer userId = claims.get("user_id", Integer.class);
            if (username == null || username.isBlank()) {
                writeAuthError(response, 321);
                return false;
            }

            // 给后续业务使用（可选）
            request.setAttribute("username", username);
            if (userId != null) request.setAttribute("user_id", userId);

            // 兼容：如果后续仍有人从 SecurityContext 里取用户名
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            return true;
        } catch (Exception e) {
            log.info("JWT invalid path={} err={}", path, e.toString());
            writeAuthError(response, 321);
            return false;
        }
    }

    private void writeAuthError(HttpServletResponse response, int code) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        String json = om.writeValueAsString(ApiResponse.error(code, null));
        response.getWriter().write(json);
    }
}

