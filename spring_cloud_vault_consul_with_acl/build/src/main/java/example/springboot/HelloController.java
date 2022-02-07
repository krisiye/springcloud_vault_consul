package example.springboot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Default Controller. 
 * 
 * @author iyerk
 */
@RestController
public class HelloController {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;
    
	@RequestMapping("/")
	public String index() {
		return "Greetings from Spring Boot! ActiveProfile: '" + activeProfile + "'!";
	}

}
