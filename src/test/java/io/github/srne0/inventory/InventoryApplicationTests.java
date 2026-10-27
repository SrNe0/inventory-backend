package io.github.srne0.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// TODO(#3): quitar exclusión temporal cuando se configure PostgreSQL + Testcontainers
@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration")
class InventoryApplicationTests {

	@Test
	void contextLoads() {
	}

}
