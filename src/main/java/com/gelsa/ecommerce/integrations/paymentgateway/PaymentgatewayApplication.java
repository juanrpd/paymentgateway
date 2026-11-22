package com.gelsa.ecommerce.integrations.paymentgateway;

import com.gelsa.ecommerce.integrations.paymentgateway.commons.dto.GatewayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableConfigurationProperties(GatewayProperties.class)
@EnableScheduling
@SpringBootApplication
public class PaymentgatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentgatewayApplication.class, args);
	}

}
