package example.springboot;

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

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;

/**
 * A basic consul KV test with test containers.
 * 
 * @author iyerk
 *
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.cloud.consul.config.enabled=true", "spring.cloud.consul.config.prefix=test/${spring.application.name}",
		"spring.cloud.consul.config.format=FILES" })
@ActiveProfiles("test")
public class ConsulKVBasicTest {

	static ConsulContainer cc;

	@BeforeAll
	public static void init() throws IOException, InterruptedException {

		String consulMasterToken = UUID.randomUUID().toString();

		Ports ports = new Ports();
		ports.setHttpPort(8501);

		cc = new ConsulContainerBuilder().withDatacenter("dc").withMasterToken(consulMasterToken)
				.withContainerVersion("1.9.5").withPorts(ports).build();

		cc.start();

		Assert.assertTrue(cc.isRunning());
		
		System.setProperty("spring.cloud.consul.host", "127.0.0.1");
		System.setProperty("spring.cloud.consul.port", String.valueOf(cc.getMappedPort(cc.getHttpPort())));
		ConsulClient client = new ConsulClient("127.0.0.1", cc.getMappedPort(cc.getHttpPort()));

		Testcontainers.exposeHostPorts(cc.getMappedPort(cc.getHttpPort()));

		String testYaml = "db:\n" + "  port: \"3307\"";

		Response<Boolean> savedWithToken = null;

		try {
			savedWithToken = client.setKVBinaryValue("test/spring-boot-example/application.yaml", testYaml.getBytes(),
					consulMasterToken, null, null);
			Assert.assertTrue(savedWithToken.getValue());
		} catch (Exception e) {
			throw e;
		}

	}

	@AfterAll
	public static void shutdown() {
		cc.stop();
	}

	@Autowired
	TestRestTemplate template;

	@Autowired
	Environment env;

	@Test
	public void test() {
		String res = template.getForObject("/config", String.class);

		// expect config values to come back from consul KV
		Assert.assertTrue(res.contains("3307"));

	}

}
