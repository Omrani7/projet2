package com.example.spring_security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private JwtFilter jwtFilter;
    
    @Autowired
    private OAuth2SuccessHandler oAuth2SuccessHandler;
    
    @Autowired
    private OAuth2FailureHandler oAuth2FailureHandler;
    
    @Autowired
    private StatelessOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    
    @Autowired
    private AppConfig appConfig;
    
    @Bean
    public AuthenticationProvider authProvider(){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(new BCryptPasswordEncoder(12));
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(customizer -> customizer.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(request -> 
                request.requestMatchers(
                    "/auth/**", 
                    "/oauth2/**", 
                    "/login/**", 
                    "/login/oauth2/code/**",
                    "/oauth2-popup-callback.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/error",
                    "/test-api/",
                    "/api/v1/institutes/**",
                    "/api/v1/properties/search",
                    "/api/v1/properties/owner-listed",
                    "/api/v1/properties/{id:[\\d]+}",
                    "/api/v1/image-proxy/**",
                    "/owner-property-images/**",
                    "/api/images/**"
                ).permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, 
                                 "/api/v1/institutes/**", 
                                 "/api/v1/properties/search",
                                 "/api/v1/properties/owner-listed",
                                 "/api/v1/properties/{id}"
                ).permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, 
                                "/api/owner-properties/**"
                ).permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/oauth2/update-role").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> 
                oauth2
                    .authorizationEndpoint(authorization -> 
                        authorization.authorizationRequestRepository(authorizationRequestRepository)
                    )
                    .successHandler(oAuth2SuccessHandler)
                    .failureHandler(oAuth2FailureHandler)
                    .userInfoEndpoint(userInfo -> 
                        userInfo.userService(new DefaultOAuth2UserService())
                    )
                    .loginPage("/login")
                    .permitAll()
            )
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exceptions -> 
                exceptions.authenticationEntryPoint((request, response, authException) -> {
                    String requestURI = request.getRequestURI();
                    if (requestURI.startsWith("/api/")) {
                        //  return 401 instead of redirecting
                        response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                    } else {
                        // For non-API requests, redirect to login
                        response.sendRedirect("/login");
                    }
                })
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(appConfig.getFrontendUrl()));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception{
        return authenticationConfiguration.getAuthenticationManager();
    }
    
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
