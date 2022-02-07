package example.springboot.config;

import static example.springboot.config.AwsConfigurationProperties.PREFIX;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ConfigurationProperties for AWS STS
 * Rebinds properties as environment variables are updated.
 * 
 * @author iyerk
 *
 */
@ConfigurationProperties(PREFIX)
@Component
public class AwsConfigurationProperties {

	private static final Logger logger = LoggerFactory.getLogger(AwsConfigurationProperties.class);
	/**
	 * Prefix for configuration properties.
	 */
	public static final String PREFIX = "cloud.aws.credentials";
	
	private String accessKey;

	private String secretKey;

	private String sessionToken;

	public String getAccesskey() {
		return accessKey;
	}

	public void setAccesskey(String accessKey) {
		this.accessKey = accessKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getSessionToken() {
		return sessionToken;
	}

	public void setSessionToken(String sessionToken) {
		this.sessionToken = sessionToken;
	}

	@PostConstruct
	private void postConstruct() {
		logger.debug(this.toString());
	}
	
	@Override
	public String toString() {
		return "AwsConfigurationProperties [accessKey=" + accessKey + ", secretKey=" + secretKey + ", sessionToken="
				+ sessionToken + "]";
	}

	
}
