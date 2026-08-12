package com.amiawake.amiawake;

import org.springframework.boot.SpringApplication;

public class TestAmIAwakeApplication {

    public static void main(String[] args) {
        SpringApplication.from(AmIAwakeApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
