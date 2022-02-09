package example.springboot.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.DefaultHealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
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

	/**
	 * Workaround for https://github.com/spring-cloud/spring-cloud-consul/issues/671
	 * @param applicationContext
	 * @param groups
	 * @return
	 */
	@Bean
	HealthContributorRegistry healthContributorRegistry(ApplicationContext applicationContext,
			HealthEndpointGroups groups) {
		Map<String, HealthContributor> healthContributors = new LinkedHashMap<>(
				applicationContext.getBeansOfType(HealthContributor.class));
		ApplicationContext parent = applicationContext.getParent();
		while (parent != null) {
			healthContributors.putAll(parent.getBeansOfType(HealthContributor.class));
			parent = parent.getParent();
		}
		return new DefaultHealthContributorRegistry(healthContributors);
	}

}
