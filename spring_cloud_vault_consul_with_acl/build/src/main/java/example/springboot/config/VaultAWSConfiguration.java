package example.springboot.config;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.event.SecretLeaseCreatedEvent;

import com.amazonaws.services.s3.AmazonS3;

/**
 * Configuration for Vault AWS secret backend for STS
 * 
 * @author iyerk
 *
 */
@Configuration
@ConditionalOnProperty(name="spring.cloud.vault.aws.enabled")
@ConditionalOnBean(SecretLeaseContainer.class)
public class VaultAWSConfiguration implements ApplicationContextAware {

	private static final Logger logger = LoggerFactory.getLogger(VaultAWSConfiguration.class);
	
	private static final String STS_PATH = "/sts/";
	
	ApplicationContext context;
	
	@Autowired
	SecretLeaseContainer container;
	
	@Autowired
	Environment env;
	
	@Value("${spring.cloud.vault.aws.backend}")
	String vaultAwsBackend;

	@Value("${spring.cloud.vault.aws.role}")
	String vaultAwsRole;
	
	@Autowired ConfigurableEnvironment configurableEnvironment;
	
	@Autowired ConfigurationPropertiesRebinder rebinder;
	
	@PostConstruct
	private void postConstruct() {
		logger.info("Register lease listener");
		
		container.addLeaseListener(leaseEvent -> {
			
			logger.debug("leaseEvent: " +leaseEvent);
			logger.debug("leaseEvent path: " +leaseEvent.getSource().getPath());
			logger.debug("leaseEvent leaseId: " +leaseEvent.getLease().getLeaseId());

			if (leaseEvent.getSource().getPath().startsWith(vaultAwsBackend + STS_PATH + vaultAwsRole) 
					&& leaseEvent instanceof SecretLeaseCreatedEvent) {

				Map<String, Object> newSecrets = ((SecretLeaseCreatedEvent) leaseEvent).getSecrets();
				
				if (logger.isDebugEnabled()) {
					newSecrets.entrySet().forEach(entry -> {
						logger.debug(entry.getKey() + " " + entry.getValue());
					});
				}
				
				// set system props and reload bean as a fall back to vault property source refresh issues.
				System.setProperty("cloud.aws.credentials.accessKey", (String)newSecrets.get("access_key"));
				System.setProperty("cloud.aws.credentials.secretKey", (String)newSecrets.get("secret_key"));
				System.setProperty("cloud.aws.credentials.sessionToken", (String)newSecrets.get("security_token"));
				
				// rebind aws configuration for this app
				rebind("awsConfigurationProperties");
				
				// refresh additional bean dependencies as needed
				refresh("AWSConfiguration");
				refresh("basicAWSCredentials");
				refresh("amazonS3Client");

				logger.info("SecretLeaseCreatedEvent received and applied for: "+leaseEvent.getSource().getPath());
			}
		});
	}
	
	private void rebind(String bean) {
		try {
			boolean success = this.rebinder.rebind(bean);
			if (logger.isInfoEnabled()) {
				logger.info(String.format(
						"Attempted to rebind bean '%s' with updated AWS secrets from vault, success: %s",
						bean, success));
			}
		}
		catch (Exception ex) {
			logger.error("Exception rebinding "+bean,ex);
		}
	}
	
	private void refresh(String bean) {
		try {
			boolean success = this.context.getBean(RefreshScope.class).refresh(bean);
			if (logger.isInfoEnabled()) {
				logger.info(String.format(
						"Attempted to refresh bean '%s' with updated AWS secrets from vault, success: %s",
						bean, success));
			}
		}
		catch (Exception ex) {
			logger.error("Exception rebinding "+bean,ex);
		}
	}	

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}	
	
}
