package br.com.example.demo;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@SecurityScheme(
		name = DemoApplication.SECURITY_SCHEME,
		type = SecuritySchemeType.HTTP,
		bearerFormat = "JWT",
		scheme = "bearer"
)
public class DemoApplication {

	public static final String SECURITY_SCHEME = "bearerAuth";

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
