package com.vasan12sp.ratelimiter.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CustomerAuthInterceptor customerAuthInterceptor;

    public WebConfig(CustomerAuthInterceptor customerAuthInterceptor) {
        this.customerAuthInterceptor = customerAuthInterceptor;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirect root and /customer to login
        registry.addRedirectViewController("/", "/customer/login");
        registry.addRedirectViewController("/customer", "/customer/login");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Static resources
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");

        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }
}
