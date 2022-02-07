package example.springboot;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.UUID;

import com.hmhco.testcontainers.consul.ConsulConfiguration.Ports;
import com.hmhco.testcontainers.consul.ConsulContainer;
import com.hmhco.testcontainers.consul.ConsulContainerBuilder;
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
import org.testcontainers.vault.VaultContainer;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;

/**
 * A test for Vault consul backend + KV with test containers.
 * 
 * @author iyerk
 *
 */

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.cloud.consul.config.enabled=true", "spring.cloud.consul.config.prefix=test/${spring.application.name}",
		"spring.cloud.consul.config.format=FILES", "spring.cloud.vault.enabled=true", "spring.cloud.vault.token=foo",
		"spring.cloud.vault.scheme=http", "spring.cloud.vault.kv.enabled=true",
		"spring.cloud.vault.consul.enabled=true", "spring.cloud.vault.consul.role=consul-read-only",
		"spring.cloud.vault.consul.backend=consul" })
@ActiveProfiles("test")
public class VaultConsulBackendTest {

	static VaultContainer vaultContainer;
	static ConsulContainer cc;

	@BeforeAll
	public static void init() throws IOException, InterruptedException {

		String consulMasterToken = UUID.randomUUID().toString();
		String consulDefaultToken = UUID.randomUUID().toString();
		String consulAgentToken = UUID.randomUUID().toString();
		String consulReplicationToken = UUID.randomUUID().toString();

		Ports ports = new Ports();
		ports.setHttpPort(8501);

		cc = new ConsulContainerBuilder().withDatacenter("dc").withACLEnabled().withMasterToken(consulMasterToken)
				.withDefaultToken(consulDefaultToken).withAgentToken(consulAgentToken)
				.withMasterToken(consulMasterToken).withReplicationToken(consulReplicationToken)
				.withACLDefaultPolicy("deny").withContainerVersion("1.9.5").withPorts(ports).build();

		cc.start();
		
		Assert.assertTrue(cc.isRunning());

		System.setProperty("spring.cloud.consul.host", "127.0.0.1");
		System.setProperty("spring.cloud.consul.port", String.valueOf(cc.getMappedPort(cc.getHttpPort())));
		ConsulClient client = new ConsulClient("127.0.0.1", cc.getMappedPort(cc.getHttpPort()));

		Testcontainers.exposeHostPorts(cc.getMappedPort(cc.getHttpPort()));

		// consul acl policy create -name read-only -rules key_prefix "" { policy =
		// "read"}
		ExecResult consulresult = cc.execInContainer("consul", "acl", "policy", "create", "-name", "read-only",
				"-rules", "key_prefix \"\" {  policy = \"read\"}", "-token", consulMasterToken,
				"-http-addr=http://127.0.0.1:8501");
		assertTrue(consulresult.getExitCode() == 0);

		String testYaml = "db:\n" + "  port: \"3307\"";

		Response<Boolean> savedWithToken = null;

		try {
			savedWithToken = client.setKVBinaryValue("test/spring-boot-example/application.yaml", testYaml.getBytes(),
					consulMasterToken, null, null);
			Assert.assertTrue(savedWithToken.getValue());
		} catch (Exception e) {
			throw e;
		}

		vaultContainer = new VaultContainer<>("vault:1.3.2").withVaultToken("foo")
				.withSecretInVault("secret/spring-boot-example/test", "password=password1");
		vaultContainer.start();
		
		Assert.assertTrue(vaultContainer.isRunning());

		// enable vault consul backend
		ExecResult result = vaultContainer.execInContainer("vault", "secrets", "enable", "consul");
		assertTrue(result.getExitCode() == 0);

		// configure consul access
		result = vaultContainer.execInContainer("vault", "write", "consul/config/access",
				"address=host.testcontainers.internal:" + cc.getMappedPort(cc.getHttpPort()),
				"token=" + consulMasterToken);
		assertTrue(result.getExitCode() == 0);

		result = vaultContainer.execInContainer("vault", "write", "consul/roles/consul-read-only",
				"policies=read-only");
		assertTrue(result.getExitCode() == 0);

		result = vaultContainer.execInContainer("vault", "read", "consul/creds/consul-read-only");
		assertTrue(result.getExitCode() == 0);

		int port = vaultContainer.getFirstMappedPort();

		System.setProperty("spring.cloud.vault.host", "127.0.0.1");
		System.setProperty("spring.cloud.vault.port", String.valueOf(port));
		System.setProperty("spring.cloud.vault.scheme", "http");
	}

	@AfterAll
	public static void shutdown() {
		vaultContainer.stop();
		cc.stop();
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

		// expect consul acl token to be available in the env seeded by vault
		Assert.assertNotNull(env.getProperty("spring.cloud.consul.config.acl-token"));

		// expect config values to come back from consul KV
		Assert.assertTrue(res.contains("3307"));

	}

}
