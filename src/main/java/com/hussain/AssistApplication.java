package com.hussain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class AssistApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssistApplication.class, args);
		log.info("==============        APPLICATION STARTED SUCCESSFULLY      ==============");
    }

}
