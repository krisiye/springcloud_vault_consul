package example.springboot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Default Application Configuration class.
 * 
 * @author iyerk
 *
 */
@Configuration
public class AppConfig {

	private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

	@EventListener(RefreshScopeRefreshedEvent.class)
	public void onRefresh(RefreshScopeRefreshedEvent event) {
		logger.info("Received " + event + " for " + event.getSource());
	}
}
