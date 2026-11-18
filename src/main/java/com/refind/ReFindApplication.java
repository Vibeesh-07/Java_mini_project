package com.refind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReFindApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReFindApplication.class, args);
    }
}
