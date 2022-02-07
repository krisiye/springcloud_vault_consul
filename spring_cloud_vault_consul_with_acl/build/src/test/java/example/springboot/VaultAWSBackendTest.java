package example.springboot;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

/**
 * A test for Vault AWS backend + KV with test containers.
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
		"spring.cloud.vault.aws.backend=aws", "cloud.aws.region=us-east1", "cloud.aws.s3.bucket=example-s3",
		"cloud.aws.s3.prefix=test-data/" })
@ActiveProfiles("test")
public class VaultAWSBackendTest {

	static VaultContainer vaultContainer;

	static LocalStackContainer localstack;

	static String sessionToken;

	@BeforeAll
	public static void init() throws IOException, InterruptedException {

		// spin up localstack with IAM
		localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.12.10"))
				.withServices(Service.IAM, Service.S3, Service.STS);
		localstack.start();

		Assert.assertTrue(localstack.isRunning());
		
		// get default creds
		String rootAccessKey = localstack.getDefaultCredentialsProvider().getCredentials().getAWSAccessKeyId();
		String rootSecretKey = localstack.getDefaultCredentialsProvider().getCredentials().getAWSSecretKey();

		// get IAM service endpoint and port
		String awsIAMEndpoint = localstack.getEndpointConfiguration(Service.IAM).getServiceEndpoint();
		int iamPort = Integer.parseInt(awsIAMEndpoint.substring(awsIAMEndpoint.lastIndexOf(":") + 1));

		// get STS service endpoint and port
		String awsStsEndpoint = localstack.getEndpointConfiguration(Service.STS).getServiceEndpoint();
		int stsPort = Integer.parseInt(awsIAMEndpoint.substring(awsStsEndpoint.lastIndexOf(":") + 1));

		// expose IAM and STS ports on the host for other containers
		Testcontainers.exposeHostPorts(iamPort);
		Testcontainers.exposeHostPorts(stsPort);

		// init vault container with kv
		vaultContainer = new VaultContainer<>("vault:1.3.2").withVaultToken("foo")
				.withSecretInVault("secret/spring-boot-example/test", "password=password1");
		vaultContainer.start();
		
		Assert.assertTrue(vaultContainer.isRunning());

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
		vaultContainer.stop();
		localstack.stop();
	}

	@Autowired
	TestRestTemplate template;

	@Autowired
	Environment env;

	@Test
	public void test() {
		String res = template.getForObject("/config", String.class);

		// expect secret token to come back from vault KV
		assertTrue(res.contains("password1"));

		// expect aws access token to be available in the env seeded by vault
		assertNotNull(env.getProperty("cloud.aws.credentials.sessionToken"));

	}

}
