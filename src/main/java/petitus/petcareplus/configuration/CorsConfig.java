package petitus.petcareplus.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
@Slf4j
public class CorsConfig {

    @Value("${cors.allowed-origins:*}")
    private String[] allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String[] allowedMethods;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration config = new CorsConfiguration();

        // Handle allowed origins
        if (allowedOrigins.length == 1 && "*".equals(allowedOrigins[0])) {
            // For production, be more specific
            config.addAllowedOriginPattern("*");
            config.addAllowedOrigin("http://localhost:3000");
            config.addAllowedOrigin("http://localhost:8080");
            config.addAllowedOrigin("http://localhost:3001");
            config.addAllowedOrigin("https://petcareplus.software");
            config.addAllowedOrigin("https://www.petcareplus.software");
            config.addAllowedOrigin("https://petcareapi.nhhtuan.id.vn");
            config.addAllowedOrigin("https://www.petcareapi.nhhtuan.id.vn");
        } else {
            for (String origin : allowedOrigins) {
                config.addAllowedOriginPattern(origin);
            }
        }

        // Add all allowed methods
        for (String method : allowedMethods) {
            config.addAllowedMethod(method);
        }

        // Add WebSocket specific headers
        config.addAllowedHeader("*");
        config.addAllowedHeader("Authorization");
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("X-Requested-With");
        config.addAllowedHeader("Accept");
        config.addAllowedHeader("Origin");
        config.addAllowedHeader("Access-Control-Request-Method");
        config.addAllowedHeader("Access-Control-Request-Headers");
        
        // WebSocket specific headers
        config.addAllowedHeader("Sec-WebSocket-Key");
        config.addAllowedHeader("Sec-WebSocket-Version");
        config.addAllowedHeader("Sec-WebSocket-Protocol");
        config.addAllowedHeader("Sec-WebSocket-Extensions");
        config.addAllowedHeader("Connection");
        config.addAllowedHeader("Upgrade");

        config.setAllowCredentials(true);
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Access-Control-Allow-Origin");
        config.addExposedHeader("Access-Control-Allow-Credentials");
        
        // Set max age for preflight requests
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        source.registerCorsConfiguration("/ws/**", config);

        log.info("CORS configuration applied with allowed origins: {}", Arrays.toString(allowedOrigins));
        log.info("CORS configuration applied with allowed methods: {}", Arrays.toString(allowedMethods));

        return new CorsFilter(source);
    }
}