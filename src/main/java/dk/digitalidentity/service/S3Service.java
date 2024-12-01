package dk.digitalidentity.service;

import dk.digitalidentity.config.GRComplianceConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;

@Service
@Slf4j
@EnableCaching
@EnableScheduling
@RequiredArgsConstructor
public class S3Service {
	private final S3Client s3Client;
    private final GRComplianceConfiguration config;

	public String upload(String filename, byte[] file) {
		String key = config.getS3().getFolderName() + "/" + filename;
		return uploadWithKey(key, file);
	}

    public String uploadWithKey(String key, byte[] file) {
        s3Client.putObject(PutObjectRequest.builder()
                .bucket(config.getS3().getBucketName())
                .key(key)
                .build(),
            RequestBody.fromBytes(file));
        return key;
    }

	public byte[] downloadBytes(String key) throws IOException {
		final String bucket = config.getS3().getBucketName();
		try {
			s3Client.headObject(HeadObjectRequest.builder()
					.bucket(bucket)
					.key(key)
					.build());
		} catch (S3Exception e) {
			if (e.statusCode() == 404) {
				log.debug("Not found. Bucket: " + bucket + " Key: " + key);
			}
			return null;
		}
		final ResponseInputStream<GetObjectResponse> response = s3Client.getObject(GetObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.build());
		return response.readAllBytes();
    }

	public void delete(String key) {
		s3Client.deleteObject(DeleteObjectRequest.builder()
				.bucket(config.getS3().getBucketName())
				.key(key)
				.build());
	}

}
