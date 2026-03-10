package com.checkout.gateway;

import com.checkout.gateway.client.BankClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class GatewayApplicationTests {

	@MockitoBean
	private BankClient bankClient;

	@Test
	void contextLoads() {
	}

}
