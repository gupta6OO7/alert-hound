package com.incident.alerthound.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AlertHoundProperties.class)
public class ApplicationConfig {
}
