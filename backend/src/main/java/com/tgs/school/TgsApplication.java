package com.tgs.school;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TgsApplication {
    public static void main(String[] args) {
        SpringApplication.run(TgsApplication.class, args);
    }
}
