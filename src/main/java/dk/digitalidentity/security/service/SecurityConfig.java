package dk.digitalidentity.security.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Profile("locallogin")
@Configuration
public class SecurityConfig {

    @Primary
    @Bean()
    public SecurityFilterChain formSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests((requests) -> requests
                .requestMatchers(
                    "/webjars/**",
                    "/manage/**",
                    "/css/**",
                    "/manage/**",
                    "/js/**",
                    "/img/**",
                    "/vendor/**",
                    "/favicon.ico",
                    "/login",
                    "/forgotten",
                    "/forgotten_sent",
                    "/reset/*",
                    "/error"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin((form) -> form
                .defaultSuccessUrl("/dashboard", true)
                .loginPage("/login").permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll());

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Primary
    @Bean
    public UserDetailsService formUserDetailsService() {
        return new FormUserDetailService();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setUserDetailsService(formUserDetailsService());
        return authProvider;
    }
}
