package com.amiawake.amiawake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class AmIAwakeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmIAwakeApplication.class, args);
    }
}
