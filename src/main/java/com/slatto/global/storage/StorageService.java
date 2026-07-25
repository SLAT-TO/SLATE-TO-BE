package com.slatto.global.storage;

import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public interface StorageService {

	void upload(MultipartFile file, String storageKey);

	ResponseInputStream<GetObjectResponse> download(String storageKey);

	void delete(String storageKey);
}
