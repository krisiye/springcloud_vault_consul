package example.springboot;

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
import org.testcontainers.vault.VaultContainer;

/**
 * A basic vault KV test with test containers.
 * 
 * @author iyerk
 *
 */

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.cloud.vault.enabled=true", "spring.cloud.vault.token=foo", "spring.cloud.vault.scheme=http",
		"spring.cloud.vault.kv.enabled=true" })
@ActiveProfiles("test")
public class VaultKVBasicTest {

	static VaultContainer vaultContainer;

	@BeforeAll
	public static void init() throws IOException, InterruptedException {

		vaultContainer = new VaultContainer<>("vault:1.3.2").withVaultToken("foo")
				.withSecretInVault("secret/spring-boot-example/test", "password=password1");
		vaultContainer.start();

		Assert.assertTrue(vaultContainer.isRunning());
		
		int port = vaultContainer.getFirstMappedPort();

		System.setProperty("spring.cloud.vault.host", "127.0.0.1");
		System.setProperty("spring.cloud.vault.port", String.valueOf(port));
		System.setProperty("spring.cloud.vault.scheme", "http");
	}

	@AfterAll
	public static void shutdown() {
		vaultContainer.stop();
	}

	@Autowired
	TestRestTemplate template;

	@Autowired
	Environment env;

	@Test
	public void test() {
		String res = template.getForObject("/config", String.class);

		// expect secret token to come back from vault KV
		Assert.assertTrue(res.contains("password1"));

	}

}
