package com.tgs.school.common.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    StoredFile store(MultipartFile file, String folder);
    Resource loadAsResource(String storagePath);
    void delete(String storagePath);

    record StoredFile(String originalFileName, String storagePath, String contentType, long size) {}
}
