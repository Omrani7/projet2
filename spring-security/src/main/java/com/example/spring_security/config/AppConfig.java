package com.example.spring_security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.PropertySource;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.modelmapper.ModelMapper;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


@Configuration
@Component
@PropertySource("classpath:application.properties")
public class AppConfig {
    
    @Value("${app.frontend-url}")
    private String frontendUrl;
    
    public String getFrontendUrl() {
        return frontendUrl;
    }
    

    public String getOAuth2CallbackUrl() {
        return frontendUrl + "/auth/oauth2-callback";
    }
    

    public String getOAuth2CallbackUrlWithToken(String token) {
        return getOAuth2CallbackUrl() + "?token=" + token;
    }
    

    public String getOAuth2CallbackUrlWithError(String errorMessage) {
        try {
            String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8.toString());
            return getOAuth2CallbackUrl() + "?error=true&message=" + encodedError;
        } catch (UnsupportedEncodingException e) {
            // Fallback in case encoding fails
            return getOAuth2CallbackUrl() + "?error=true&message=Authentication+failed";
        }
    }


    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(java.time.Duration.ofSeconds(10))
            .setReadTimeout(java.time.Duration.ofSeconds(30))
            .requestFactory(this::clientHttpRequestFactory)
            .build();
    }

    @Bean
    public SimpleClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds
        factory.setReadTimeout(30000);    // 30 seconds
        factory.setBufferRequestBody(false); // For large responses like images
        return factory;
    }
} 