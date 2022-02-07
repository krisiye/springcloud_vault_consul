package example.springboot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * A shutdown hook for any cleanup before shutdown.
 * 
 * @author iyerk
 *
 */
@Component
public class ApplicationShutdownHook implements ApplicationListener<ContextClosedEvent> {

	private static final Logger logger = LoggerFactory.getLogger(ApplicationShutdownHook.class);
	
     @Override
     public void onApplicationEvent(ContextClosedEvent event) {
         
    	 logger.info("Received ContextClosedEvent. Process any clean up before shutddown here. Sleeping for 5 seconds.");
    	 
    	 try {
    		 Thread.sleep(5000);
    	 } catch (Exception ex) {
    		 // do nothing for now
    	 }
     }
}
