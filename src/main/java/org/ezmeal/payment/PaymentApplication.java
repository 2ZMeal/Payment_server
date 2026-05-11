package org.ezmeal.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;




@EnableScheduling
@EnableFeignClients

@SpringBootApplication(scanBasePackages = {"com.ezmeal.payment", "com.ezmeal.common"})
@EntityScan(basePackages = {"com.ezmeal.payment", "com.ezmeal.common"})
@EnableJpaRepositories(basePackages = {"com.ezmeal.payment", "com.ezmeal.common"})
public class PaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }

}
