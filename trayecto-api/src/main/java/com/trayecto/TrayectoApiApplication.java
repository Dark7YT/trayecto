package com.trayecto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Modulithic(
    systemName = "Trayecto",
    sharedModules = "shared"
)
@ConfigurationPropertiesScan("com.trayecto")
@EnableAsync
@EnableScheduling
public class TrayectoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrayectoApiApplication.class, args);
    }
}
