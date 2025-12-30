package com.cinehub.profile.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LanguageInterceptor implements HandlerInterceptor {

    private static final ThreadLocal<String> languageContext = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Lấy language từ header Accept-Language
        String language = request.getHeader("Accept-Language");
        
        // Mặc định là "vi" nếu không có header
        if (language == null || language.trim().isEmpty()) {
            language = "vi";
        }
        
        // Validate language (chỉ chấp nhận "vi" hoặc "en")
        if (!"vi".equals(language) && !"en".equals(language)) {
            language = "vi";
        }
        
        // Lưu vào ThreadLocal
        languageContext.set(language);
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Clear ThreadLocal để tránh memory leak
        languageContext.remove();
    }

    // Static method để lấy language từ bất kỳ đâu
    public static String getCurrentLanguage() {
        String language = languageContext.get();
        return language != null ? language : "vi";
    }

    // Check if current language is English
    public static boolean isEnglish() {
        return "en".equals(getCurrentLanguage());
    }
}