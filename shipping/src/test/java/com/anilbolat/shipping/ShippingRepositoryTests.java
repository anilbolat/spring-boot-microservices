package com.anilbolat.shipping;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.fail;

@Testcontainers
@SpringBootTest
//@ActiveProfiles("tc")
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ShippingRepositoryTests {

    @Autowired
    private ShippingRepository shippingRepository;

    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:11.1")
            .withDatabaseName("integration-tests-db")
            .withUsername("ab")
            .withPassword("ab");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @Test
    @Transactional
    void testShouldBeFoundWhenShippingAreSaved() {
        Shipping s1 = new Shipping();
        s1.setOrderId(10);
        shippingRepository.save(s1);

        for (Shipping shipping : shippingRepository.findAll()) {
            if (shipping.getOrderId() != 10) {
                fail("should be 10");
            }
        }
    }
}

