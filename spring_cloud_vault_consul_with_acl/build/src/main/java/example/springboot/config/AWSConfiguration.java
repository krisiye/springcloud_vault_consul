package example.springboot.config;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

/**
 * AWS configuration class to initialize aws client sdk.
 * 
 * @author iyerk
 *
 */
@Configuration
@ConditionalOnProperty(name="spring.cloud.vault.aws.enabled")
@RefreshScope
public class AWSConfiguration {
	
	private static final Logger logger = LoggerFactory.getLogger(AWSConfiguration.class);

	@Autowired
	AwsConfigurationProperties awsConfigurationProperties;
	
	@Value("${cloud.aws.region}")
	private String region;
	
	@Value("${cloud.aws.s3.endpoint:NOT_DEFINED}")
	private String s3Endpoint;

	@Bean
	@RefreshScope
	public AWSSessionCredentials basicAWSCredentials() {
		AWSSessionCredentials credentials = new BasicSessionCredentials(awsConfigurationProperties.getAccesskey(),
				awsConfigurationProperties.getSecretKey(), awsConfigurationProperties.getSessionToken());
		return credentials;
		
	}

	@Bean
	@RefreshScope
	@Profile("!test & !local")
	public AmazonS3 amazonS3Client(AWSCredentials awsCredentials) {
		
		AmazonS3 s3client = AmazonS3ClientBuilder
				  .standard()
				  .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
				  .withRegion(region)
				  .build();
		return s3client;
	}
	
	@Bean
	@RefreshScope
	@Profile("test || local")
	public AmazonS3 amazonTestS3Client(AWSCredentials awsCredentials) {
		
		AmazonS3 s3client;
		
		// Init your AmazonS3 credentials using BasicAWSCredentials for local and test profiles
		// see  https://github.com/localstack/localstack#setting-up-local-region-and-credentials-to-run-localstack		
        BasicAWSCredentials credentials = new BasicAWSCredentials("test", "test");
		
		if (s3Endpoint.contentEquals("NOT_DEFINED"))
			s3client = AmazonS3ClientBuilder
				  .standard()
				  .withCredentials(new DefaultAWSCredentialsProviderChain())
				  .withRegion(region)
				  .build();
		else
			s3client = AmazonS3ClientBuilder
			  .standard()
			  .withCredentials(new AWSStaticCredentialsProvider(credentials))
			  .withEndpointConfiguration(new EndpointConfiguration(s3Endpoint, region))
			  .enablePathStyleAccess()
			  .build();
		
		return s3client;
	}
	
	@PostConstruct
	private void postConstruct() {
		logger.info("AWSConfiguration refreshed");
	}
	
	
}
