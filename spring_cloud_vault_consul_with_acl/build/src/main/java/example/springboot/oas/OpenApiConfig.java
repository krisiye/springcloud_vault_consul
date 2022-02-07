package example.springboot.oas;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * Spring Doc configuration for OpenAPI
 * 
 * @author iyerk
 *
 */
@Configuration
public class OpenApiConfig {
 
    @Bean
    public OpenAPI customOpenAPI() {

        return new OpenAPI()
                .info(new Info().title("Spring Boot Example").description(
                        "OpenAPI 3 spec for Spring Boot Example."));
    }
    

}