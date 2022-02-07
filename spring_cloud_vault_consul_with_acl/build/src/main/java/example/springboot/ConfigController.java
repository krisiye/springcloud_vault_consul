package example.springboot;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Config Demo Controller.
 *  
 * @author iyerk
 * 
 */
@RestController
@RefreshScope
public class ConfigController {

	
	@Value("${db.port:NOT_DEFINED}")
	String dbPort;
	
	// read secret from vault
	@Value("${password:NOT_DEFINED}")
	String superSecretToken;	
    
	@GetMapping(path = "/config")
	public Map<String,String> getConfig() {

        HashMap<String, String> map = new HashMap<>();
        map.put("dbport", dbPort);
        map.put("superSecretToken", superSecretToken);
        return map;
	}

}
