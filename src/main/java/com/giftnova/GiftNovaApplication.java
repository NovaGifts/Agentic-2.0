package com.giftnova;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GiftNovaApplication {
    public static void main(String[] args) {
        SpringApplication.run(GiftNovaApplication.class, args);
    }
}
