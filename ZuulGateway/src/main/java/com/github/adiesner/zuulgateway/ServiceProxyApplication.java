package com.github.adiesner.zuulgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDiscoveryClient
@EnableZuulProxy
public class ServiceProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceProxyApplication.class, args);
	}

	@Bean
	public ServiceViaBranchCookiePreFilter simpleFilter() {
		return new ServiceViaBranchCookiePreFilter();
	}
}
