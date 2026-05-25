package com.trayecto;

import org.springframework.boot.SpringApplication;

public class TestTrayectoApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(TrayectoApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
