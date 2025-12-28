package io.github.hyperf0rm.deep_vibe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DeepVibeApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeepVibeApplication.class, args);
	}

}
