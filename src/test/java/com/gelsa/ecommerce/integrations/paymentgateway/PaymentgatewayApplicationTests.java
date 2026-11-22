package com.gelsa.ecommerce.integrations.paymentgateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PaymentgatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
