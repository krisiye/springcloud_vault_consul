package example.springboot.s3;

import java.net.URL;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * A s3 wrapper to do basic operations on S3.
 *  
 * @author iyerk
 *
 */
@Service
@ConditionalOnProperty(name="spring.cloud.vault.aws.enabled")
public class S3Wrapper {

	@Autowired
	private AmazonS3 amazonS3Client;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	@Value("${cloud.aws.s3.prefix}")
	private String prefix;

	public List<S3ObjectSummary> list() {
		ObjectListing objectListing = amazonS3Client.listObjects(
				new ListObjectsRequest().withBucketName(bucket).withPrefix(prefix));

		List<S3ObjectSummary> s3ObjectSummaries = objectListing.getObjectSummaries();

		return s3ObjectSummaries;
	}

	public String preSign(String key, Integer duration) {

		var date = new Date(new Date().getTime() + duration * 1000);
		
		URL preSignedURL = amazonS3Client.generatePresignedUrl(bucket, key, date, HttpMethod.GET);

		return preSignedURL.toString();
	}	
}