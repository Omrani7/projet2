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
import org.springframework.context.annotation.Primary;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;


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
    

    public String getOAuth2PopupCallbackUrlWithToken(String token) {
        return "/oauth2-popup-callback.html?token=" + token;
    }

    public String getOAuth2PopupCallbackUrlWithError(String errorMessage) {
        try {
            String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8.toString());
            return "/oauth2-popup-callback.html?error=true&message=" + encodedError;
        } catch (UnsupportedEncodingException e) {
            return "/oauth2-popup-callback.html?error=true&message=Authentication+failed";
        }
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
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(60)) // 60 S
                .build();
    }

    /**
     rest mtaa el scraper ki ytawell fel khedma
     */
    @Bean("scraperRestTemplate")
    public RestTemplate scraperRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(15))
                .setReadTimeout(Duration.ofMinutes(10)) // 10 min
                .build();
    }

    @Bean
    public SimpleClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 s
        factory.setReadTimeout(30000);    // 30 s
        factory.setBufferRequestBody(false); // for images
        return factory;
    }
} 