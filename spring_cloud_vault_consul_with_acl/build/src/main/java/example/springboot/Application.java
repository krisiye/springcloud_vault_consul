package example.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main class for SB.
 * 
 * @author iyerk
 *
 */
@SpringBootApplication
@EnableAutoConfiguration
@EnableConfigurationProperties
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	
}
