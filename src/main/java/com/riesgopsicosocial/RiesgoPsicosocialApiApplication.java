package com.riesgopsicosocial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RiesgoPsicosocialApiApplication {

    public static void main(String[] args) {

		SpringApplication.run(RiesgoPsicosocialApiApplication.class, args);
    }

}
