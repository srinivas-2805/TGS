package com.tgs.school.common.storage;

import com.tgs.school.common.exception.BadRequestException;
import com.tgs.school.common.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Local filesystem implementation of StorageService.
 * Cloud object storage (e.g., AWS S3) can be plugged in via the same StorageService interface.
 */
@Service
@RequiredArgsConstructor
public class LocalStorageService implements StorageService {

    private final FileUploadValidator validator;

    @Value("${app.storage.local-dir:./uploads}")
    private String localDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(localDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not initialize upload storage directory", ex);
        }
    }

    @Override
    public StoredFile store(MultipartFile file, String folder) {
        validator.validate(file);
        String cleanOriginal = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String safeFolder = folder.replaceAll("[^a-zA-Z0-9/_-]", "");
        String ext = cleanOriginal.contains(".") ? cleanOriginal.substring(cleanOriginal.lastIndexOf('.')) : "";
        String storedName = UUID.randomUUID() + ext;

        try {
            Path targetDir = rootLocation.resolve(safeFolder).normalize();
            if (!targetDir.startsWith(rootLocation)) {
                throw new BadRequestException("Invalid storage folder path.");
            }
            Files.createDirectories(targetDir);
            Path destination = targetDir.resolve(storedName).normalize();
            if (!destination.startsWith(rootLocation)) {
                throw new BadRequestException("Invalid file destination path.");
            }
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            String relativePath = rootLocation.relativize(destination).toString().replace('\\', '/');
            return new StoredFile(cleanOriginal, relativePath, file.getContentType(), file.getSize());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store uploaded file", ex);
        }
    }

    @Override
    public Resource loadAsResource(String storagePath) {
        try {
            Path file = rootLocation.resolve(storagePath).normalize();
            if (!file.startsWith(rootLocation) || !Files.exists(file)) {
                throw new ResourceNotFoundException("File not found: " + storagePath);
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("File not readable: " + storagePath);
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("Invalid file path");
        }
    }

    @Override
    public void delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) return;
        try {
            Path file = rootLocation.resolve(storagePath).normalize();
            if (file.startsWith(rootLocation)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException ignored) {
        }
    }
}
