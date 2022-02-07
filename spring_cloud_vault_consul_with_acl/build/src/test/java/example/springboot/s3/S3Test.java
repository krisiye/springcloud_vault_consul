package example.springboot.s3;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StreamUtils;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;

/**
 * A Spring boot test for AWS S3 integration with LocalStack Note: this does not
 * test for S3 policy and IAM/STS. The test focuses on Spring clould AWS and S3
 * integration for the most part.
 * 
 * @author iyerk
 *
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.cloud.vault.enabled=true", "spring.cloud.vault.token=foo", "spring.cloud.vault.scheme=http",
		"spring.cloud.vault.kv.enabled=true", "spring.cloud.vault.aws.enabled=true",
		"spring.cloud.vault.aws.role=readonly-examples", "spring.cloud.vault.aws.credential-type=assumed_role",
		"spring.cloud.vault.aws.access-key-property=cloud.aws.credentials.accessKey",
		"spring.cloud.vault.aws.secret-key-property=cloud.aws.credentials.secretKey",
		"spring.cloud.vault.aws.backend=aws", "cloud.aws.s3.bucket=testbucket", "cloud.aws.s3.prefix=test-data/" })
@ActiveProfiles("test")
public class S3Test {

	static VaultContainer vaultContainer;

	static LocalStackContainer s3Container;

	@Autowired
	AmazonS3 amazonS3;

	@BeforeAll
	public static void init() throws IOException, InterruptedException {

		s3Container = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.12.10"))
				.withServices(Service.IAM, Service.S3, Service.STS);
		// start localstack
		s3Container.start();

		// get default creds
		String rootAccessKey = s3Container.getDefaultCredentialsProvider().getCredentials().getAWSAccessKeyId();
		String rootSecretKey = s3Container.getDefaultCredentialsProvider().getCredentials().getAWSSecretKey();

		System.setProperty("aws.accessKeyId",
				s3Container.getDefaultCredentialsProvider().getCredentials().getAWSAccessKeyId());
		System.setProperty("aws.secretKey",
				s3Container.getDefaultCredentialsProvider().getCredentials().getAWSSecretKey());
		System.setProperty("cloud.aws.s3.endpoint", s3Container.getEndpointConfiguration(Service.S3).getServiceEndpoint());
		System.setProperty("s3.region", s3Container.getEndpointConfiguration(Service.S3).getSigningRegion());
		System.setProperty("cloud.aws.region", s3Container.getEndpointConfiguration(Service.S3).getSigningRegion());

		// get IAM service endpoint and port
		String awsIAMEndpoint = s3Container.getEndpointConfiguration(Service.IAM).getServiceEndpoint();
		int iamPort = Integer.parseInt(awsIAMEndpoint.substring(awsIAMEndpoint.lastIndexOf(":") + 1));

		// get STS service endpoint and port
		String awsStsEndpoint = s3Container.getEndpointConfiguration(Service.STS).getServiceEndpoint();
		int stsPort = Integer.parseInt(awsIAMEndpoint.substring(awsStsEndpoint.lastIndexOf(":") + 1));

		// expose IAM and STS ports on the host for other containers
		Testcontainers.exposeHostPorts(iamPort);
		Testcontainers.exposeHostPorts(stsPort);

		// init vault container with kv
		vaultContainer = new VaultContainer<>("vault:1.3.2").withVaultToken("foo")
				.withSecretInVault("secret/spring-boot-example/test", "password=password1");
		vaultContainer.start();

		// enable vault aws backend
		ExecResult result = vaultContainer.execInContainer("vault", "secrets", "enable", "aws");
		assertNotNull(result);
		assertTrue(result.getExitCode() == 0);

		// configure aws access for vault aws backend with custom iam and sts endpoints
		result = vaultContainer.execInContainer("vault", "write", "aws/config/root", "access_key=" + rootAccessKey,
				"secret_key=" + rootSecretKey, "region=us-east-1",
				"iam_endpoint=http://host.testcontainers.internal:" + iamPort,
				"sts_endpoint=http://host.testcontainers.internal:" + stsPort);
		assertNotNull(result);
		assertTrue(result.getExitCode() == 0);

		// deploy policy
		// vault write aws/roles/readonly-examples \
		// role_arns=arn:aws:iam::ACCOUNT-ID-WITHOUT-HYPHENS:role/RoleNameToAssume \
		// credential_type=assumed_role
		result = vaultContainer.execInContainer("vault", "write", " aws/roles/readonly-examples",
				"role_arns=arn:aws:iam::foo:role/readonly-examples", "credential_type=assumed_role");
		assertNotNull(result);
		assertTrue(result.getExitCode() == 0);

		// To generate a new set of STS assumed role credentials, we again write to the
		// role using the aws/sts endpoint
		// vault write aws/sts/readonly-examples ttl=60m
		result = vaultContainer.execInContainer("vault", "write", " aws/sts/readonly-examples", "ttl=60m");
		assertNotNull(result);
		assertTrue(result.getExitCode() == 0);

		// expected output

		// --- -----
		// lease_id aws/sts/readonly-examples/CqvqeWeYmmiJGpkD6iFuoKxM
		// lease_duration 1h
		// lease_renewable false
		// access_key ASIA8NWMTLYQHT5J64NZ
		// secret_key n+3AteNvvVOHK7b+EL9JiD/IKV+h4zrPhFxeRGSW
		// security_token FQoGZXIvYXdzEBYaD3WZ+t6hqJeMpnovTVv2xvd8osz....

		assertTrue(result.toString().contains("security_token"));

		// set up system props for vault
		System.setProperty("spring.cloud.vault.host", "127.0.0.1");
		System.setProperty("spring.cloud.vault.port", String.valueOf(vaultContainer.getFirstMappedPort()));
		System.setProperty("spring.cloud.vault.scheme", "http");
	}

	@AfterAll
	public static void shutdown() {
		s3Container.stop();
		vaultContainer.stop();
	}

	@Autowired
	TestRestTemplate template;

	@Autowired
	Environment env;

	@Test
	public void test() throws IOException, InterruptedException {

		// create a bucket and put a file
		amazonS3.createBucket("testbucket");
		amazonS3.putObject("testbucket", "test-data/test.txt", "bar");

		// get object via aws sdk
		S3Object s3Obj = amazonS3.getObject("testbucket", "test-data/test.txt");
		assertNotNull(s3Obj);

		// verify object content
		String objContents = StreamUtils.copyToString(s3Obj.getObjectContent(), StandardCharsets.UTF_8);
		assertTrue(objContents.contentEquals("bar"));

		// list bucket via Spring cloud API
		String res = template.getForObject("/aws/s3/list", String.class);

		// expect key to be listed on the list bucket response
		assertTrue(res.contains("test-data/test.txt"));

	}
	
	@Test
	public void testPreSign() throws IOException, InterruptedException {

		// create a bucket and put a file
		amazonS3.createBucket("testbucket");
		amazonS3.putObject("testbucket", "test-data/test1.txt", "bar");

		// get object via aws sdk
		S3Object s3Obj = amazonS3.getObject("testbucket", "test-data/test1.txt");
		assertNotNull(s3Obj);

		// verify object content
		String objContents = StreamUtils.copyToString(s3Obj.getObjectContent(), StandardCharsets.UTF_8);
		assertTrue(objContents.contentEquals("bar"));

		// list bucket via Spring cloud API
		String res = template.getForObject("/aws/s3/list", String.class);
		
		// expect key to be listed on the list bucket response
		assertTrue(res.contains("test-data/test1.txt"));

		// presign
		String preSignURL = template.getForObject("/aws/s3/presign?key=test-data/test1.txt", String.class);
		assertNotNull(preSignURL);
		
		// download file through presign.
	    try (InputStream inputStream = new URL(preSignURL).openStream())
	    {
	        String file =  IOUtils.toString(inputStream);
	        assertTrue(file.contentEquals("bar"));
	    }		
		

	}	

}
