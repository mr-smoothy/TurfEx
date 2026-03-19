package com.turfexplorer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SslcommerzConfigHealthLogger implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(SslcommerzConfigHealthLogger.class);

    @Value("${ssl.base.url:}")
    private String baseUrl;

    @Value("${ssl.store.id:}")
    private String storeId;

    @Value("${ssl.store.password:}")
    private String storePassword;

    @Value("${ssl.success.url:}")
    private String successUrl;

    @Value("${ssl.fail.url:}")
    private String failUrl;

    @Value("${ssl.cancel.url:}")
    private String cancelUrl;

    @Override
    public void run(ApplicationArguments args) {
        List<String> missing = new ArrayList<>();
        if (isBlank(baseUrl)) {
            missing.add("SSLCOMMERZ_BASE_URL");
        }
        if (isBlank(storeId)) {
            missing.add("SSLCOMMERZ_STORE_ID");
        }
        if (isBlank(storePassword)) {
            missing.add("SSLCOMMERZ_STORE_PASSWORD");
        }
        if (isBlank(successUrl)) {
            missing.add("SSLCOMMERZ_SUCCESS_URL");
        }
        if (isBlank(failUrl)) {
            missing.add("SSLCOMMERZ_FAIL_URL");
        }
        if (isBlank(cancelUrl)) {
            missing.add("SSLCOMMERZ_CANCEL_URL");
        }

        if (missing.isEmpty()) {
            logger.info("SSLCOMMERZ configuration check passed. Required settings are present.");
            return;
        }

        logger.warn("SSLCOMMERZ configuration is incomplete. Missing env vars: {}", String.join(", ", missing));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
