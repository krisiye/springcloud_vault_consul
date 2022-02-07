package example.springboot.config;

import java.util.Collections;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.vault.config.consul.VaultConsulProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.env.VaultPropertySource;

/**
 * A workaround for making sure spring.cloud.consul.token is injected
 * into the environment in time for spring cloud config consul to pick up.
 * 
 * https://github.com/spring-cloud/spring-cloud-vault/issues/607
 * https://github.com/spring-projects/spring-boot/issues/28849
 * 
 * @author iyerk
 *
 */
@Configuration
@AutoConfigureOrder(1)
@ConditionalOnProperty(value = "spring.cloud.vault.enabled", matchIfMissing = true)
public class VaultForConsulBootstrapConfiguration implements ApplicationContextAware,
		InitializingBean {

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() {

		ConfigurableEnvironment ce = (ConfigurableEnvironment) applicationContext
				.getEnvironment();

		if (ce.getPropertySources().contains("consul-token")) {
			return;
		}

		VaultOperations vaultOperations = applicationContext
				.getBean(VaultOperations.class);
		VaultConsulProperties consulProperties = applicationContext
				.getBean(VaultConsulProperties.class);

		VaultPropertySource vaultPropertySource = new VaultPropertySource(
				vaultOperations, String.format("%s/creds/%s",
						consulProperties.getBackend(), consulProperties.getRole()));

		MapPropertySource mps = new MapPropertySource("consul-token",
				Collections.singletonMap("spring.cloud.consul.token",
						vaultPropertySource.getProperty("token")));

		ce.getPropertySources().addFirst(mps);
	}
}