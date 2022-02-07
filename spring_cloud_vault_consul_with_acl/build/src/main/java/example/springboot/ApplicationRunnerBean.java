package example.springboot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

/**
 * Sample ApplicationRunner to run start up routines
 * before the container can start accepting traffic (marked ready)
 * 
 * @author iyerk
 *
 */
@Component
public class ApplicationRunnerBean implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationRunnerBean.class);
    
	@Autowired
	BuildProperties properties;
	
    @Override
    public void run(ApplicationArguments arg0) throws Exception {
        logger.info("ApplicationRunnerBean .. Sleeping for 1 second. Run your startup code here before we mark container to be ready.");
        Thread.sleep(1000);
        
        // do useful things here. 
        
        // print artifact details
        properties.forEach(e -> logger.info("BuildProperties: " + e.getKey() + ":" + e.getValue()));
        
    }
}