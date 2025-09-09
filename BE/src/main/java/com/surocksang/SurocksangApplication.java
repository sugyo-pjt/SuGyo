package com.surocksang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class SurocksangApplication {

    public static void main(String[] args) {
        SpringApplication.run(SurocksangApplication.class, args);
    }

}
