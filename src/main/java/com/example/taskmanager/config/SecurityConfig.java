package com.example.taskmanager.config;

import com.example.taskmanager.security.CustomAccessDeniedHandler;
import com.example.taskmanager.security.CustomAuthEntryPoint;
import com.example.taskmanager.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomAuthEntryPoint customAuthEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          CustomAuthEntryPoint customAuthEntryPoint,
                          CustomAccessDeniedHandler customAccessDeniedHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.customAuthEntryPoint = customAuthEntryPoint;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var authApi = PathPatternRequestMatcher.withDefaults().matcher("/api/auth/**");
        var h2 = PathPatternRequestMatcher.withDefaults().matcher("/h2-console/**");
        var error = PathPatternRequestMatcher.withDefaults().matcher("/error");
        var loginPage = PathPatternRequestMatcher.withDefaults().matcher("/login");
        var registerPage = PathPatternRequestMatcher.withDefaults().matcher("/register");
        var staticCss = PathPatternRequestMatcher.withDefaults().matcher("/css/**");
        var staticJs = PathPatternRequestMatcher.withDefaults().matcher("/js/**");
        var favicon = PathPatternRequestMatcher.withDefaults().matcher("/favicon.ico");

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(authApi, h2, error, loginPage, registerPage,
                                staticCss, staticJs, favicon).permitAll()
                        .requestMatchers("/web/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/web/tasks", true)
                        .permitAll()
                )
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .headers(headers -> headers.frameOptions(fo -> fo.disable()))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}