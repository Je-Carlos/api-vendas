package com.exemplo.fornecedoresservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class FornecedoresServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FornecedoresServiceApplication.class, args);
    }
}
