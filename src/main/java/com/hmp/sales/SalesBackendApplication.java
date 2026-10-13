package com.hmp.sales;

import com.hmp.sales.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class SalesBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SalesBackendApplication.class, args);
	}

}
