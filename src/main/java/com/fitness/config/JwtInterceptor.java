package com.fitness.config;

import com.alibaba.fastjson2.JSONObject;
import com.fitness.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 拦截器
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行登录和注册接口
        String uri = request.getRequestURI();
        if (uri.contains("/user/login") || uri.contains("/user/register")) {
            return true;
        }
        
        // 获取请求头中的 token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSONObject.of("code", 401, "msg", "请先登录").toString());
            return false;
        }
        
        // 去除 Bearer 前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // 验证 token
        if (!jwtUtil.validateToken(token) || jwtUtil.isTokenExpired(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSONObject.of("code", 401, "msg", "登录已过期，请重新登录").toString());
            return false;
        }
        
        // 将用户ID与角色存入 request 属性中，供后续使用
        Long userId = jwtUtil.getUserIdFromToken(token);
        request.setAttribute("userId", userId);
        request.setAttribute("userRole", jwtUtil.getRoleFromToken(token));
        
        // 管理员专属路径校验：/admin/** 仅管理员可访问
        // 放在这里统一拦截，避免每个 Controller 方法重复判断
        if (uri.startsWith("/admin/")) {
            Integer role = (Integer) request.getAttribute("userRole");
            if (role == null || role != 1) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(JSONObject.of("code", 403, "msg", "无管理员权限").toString());
                return false;
            }
        }
        
        return true;
    }
}
