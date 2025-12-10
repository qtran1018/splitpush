package com.splitpush;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SplitpushApplication {

    public static void main(String[] args) {
        SpringApplication.run(SplitpushApplication.class, args);
    }
}

