package com.pulsewatch.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootApplication
public class PulsewatchBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PulsewatchBackendApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx, RequestMappingHandlerMapping handlerMapping) {
		return args -> {
			System.out.println("===== REGISTERED BEANS =====");
			for (String beanName : ctx.getBeanNamesForAnnotation(org.springframework.web.bind.annotation.RestController.class)) {
				System.out.println("RestController: " + beanName);
			}
			System.out.println("===== REGISTERED MAPPINGS =====");
			handlerMapping.getHandlerMethods().forEach((key, value) -> {
				System.out.println("Mapped path: " + key + " -> " + value);
			});
		};
	}
}
