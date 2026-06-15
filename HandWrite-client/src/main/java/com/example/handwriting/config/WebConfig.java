package com.example.handwriting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 与 CORS 配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    public WebConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        AppProperties.Cors cors = appProperties.getCors();
        for (String origin : cors.getAllowedOrigins().split(",")) {
            String trimmed = origin.trim();
            if ("*".equals(trimmed)) {
                cfg.addAllowedOriginPattern("*");
            } else if (!trimmed.isEmpty()) {
                cfg.addAllowedOrigin(trimmed);
            }
        }
        for (String method : cors.getAllowedMethods().split(",")) {
            cfg.addAllowedMethod(method.trim());
        }
        for (String header : cors.getAllowedHeaders().split(",")) {
            cfg.addAllowedHeader(header.trim());
        }
        cfg.setAllowCredentials(cors.isAllowCredentials());
        cfg.setMaxAge(cors.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return new CorsFilter(source);
    }
}
