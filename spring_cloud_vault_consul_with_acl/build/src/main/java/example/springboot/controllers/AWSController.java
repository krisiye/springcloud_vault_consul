package example.springboot.controllers;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amazonaws.services.s3.model.S3ObjectSummary;

import example.springboot.s3.S3Wrapper;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * AWS Controller for AWS related REST operations.
 * 
 * @author iyerk
 *
 */

@RestController
@RequestMapping("/aws")
@ConditionalOnProperty(name="spring.cloud.vault.aws.enabled")
public class AWSController {

	@Autowired
	private S3Wrapper s3Wrapper;

	@GetMapping(value = "/s3/list")
	public List<S3ObjectSummary> list() throws IOException {
		return s3Wrapper.list();
	}
	
	@GetMapping(value = "/s3/presign")
	public String preSign(@Parameter(description = "s3 object key")  @RequestParam(required = true) String key, 
			@Parameter(description = "duration in seconds") @RequestParam(defaultValue = "300") Integer duration) throws IOException {
		return s3Wrapper.preSign(key, duration);
	}	
}