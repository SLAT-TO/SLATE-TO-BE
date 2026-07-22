package com.slatto.global.storage;

import com.slatto.global.config.properties.S3Properties;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

	private final S3Client s3Client;
	private final S3Properties s3Properties;

	@Override
	public void upload(MultipartFile file, String storageKey) {
		try (InputStream inputStream = file.getInputStream()) {
			PutObjectRequest request = PutObjectRequest.builder()
				.bucket(s3Properties.bucket())
				.key(storageKey)
				.contentType(file.getContentType())
				.contentLength(file.getSize())
				.build();

			s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
		} catch (IOException | SdkException exception) {
			log.warn("S3 file upload failed. storageKey={}", storageKey, exception);
			throw new BaseException(CommonErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseInputStream<GetObjectResponse> download(String storageKey) {
		try {
			GetObjectRequest request = GetObjectRequest.builder()
				.bucket(s3Properties.bucket())
				.key(storageKey)
				.build();

			return s3Client.getObject(request);
		} catch (SdkException exception) {
			log.warn("S3 file download failed. storageKey={}", storageKey, exception);
			throw new BaseException(CommonErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public void delete(String storageKey) {
		try {
			s3Client.deleteObject(request -> request
				.bucket(s3Properties.bucket())
				.key(storageKey)
			);
		} catch (SdkException exception) {
			log.warn("S3 file delete failed. storageKey={}", storageKey, exception);
			throw new BaseException(CommonErrorCode.INTERNAL_SERVER_ERROR);
		}
	}
}
