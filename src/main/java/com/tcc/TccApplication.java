package com.tcc;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TccApplication {

	public static void main(String[] args) {
		String timezone = System.getenv().getOrDefault("APP_TIMEZONE", "America/Sao_Paulo");
		TimeZone.setDefault(TimeZone.getTimeZone(timezone));
		System.setProperty("user.timezone", timezone);
		SpringApplication.run(TccApplication.class, args);
	}

}
