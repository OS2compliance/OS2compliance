package dk.digitalidentity.config;

import dk.digitalidentity.security.ApiSecurityFilter;
import dk.digitalidentity.service.ApiClientService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiSecurityFilterConfiguration {
    private final ApiClientService apiClientService;

    public ApiSecurityFilterConfiguration(final ApiClientService apiClientService) {
        this.apiClientService = apiClientService;
    }

    @Bean
    public FilterRegistrationBean<ApiSecurityFilter> apiSecurityFilter() {
        final ApiSecurityFilter filter = new ApiSecurityFilter(apiClientService);
        final FilterRegistrationBean<ApiSecurityFilter> filterRegistrationBean = new FilterRegistrationBean<>(filter);
        filterRegistrationBean.addUrlPatterns("/api/*");
        filterRegistrationBean.setOrder(100);
        return filterRegistrationBean;
    }
}
