package com.flex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
		scanBasePackages = {
				"com.flex",
				"com.flex.common_module",
				"com.flex.service_module",
				"com.flex.user_module",
				"com.flex.job_module",
				"com.flex.dashboard_module",
				"com.flex.notification_module"
		}
)
@EnableCaching
@EnableScheduling
public class ServiceGatewayBackendApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(ServiceGatewayBackendApplication.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(ServiceGatewayBackendApplication.class);
	}
}
