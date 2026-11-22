package com.gelsa.ecommerce.integrations.paymentgateway;

import org.springframework.boot.SpringApplication;

public class TestPaymentgatewayApplication {

	public static void main(String[] args) {
		SpringApplication.from(PaymentgatewayApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
