package com.vasan12sp.ratelimiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class RateLimiterApplication {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterApplication.class);

    private final Environment environment;

    public RateLimiterApplication(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(RateLimiterApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String port = environment.getProperty("server.port", "8080");
        String redisHost = environment.getProperty("spring.data.redis.host", "localhost");
        String redisPort = environment.getProperty("spring.data.redis.port", "6379");

        logger.info("=================================================");
        logger.info("  Distributed Rate Limiter Service Started");
        logger.info("=================================================");
        logger.info("  Server running on port: {}", port);
        logger.info("  Redis connection: {}:{}", redisHost, redisPort);
        logger.info("-------------------------------------------------");
        logger.info("  API Endpoints:");
        logger.info("    POST /v1/rate-limit/check - Check rate limit");
        logger.info("-------------------------------------------------");
        logger.info("  Admin Console:");
        logger.info("    http://localhost:{}/admin", port);
        logger.info("-------------------------------------------------");
        logger.info("  Metrics:");
        logger.info("    http://localhost:{}/actuator/prometheus", port);
        logger.info("    http://localhost:{}/actuator/health", port);
        logger.info("=================================================");
    }
}
