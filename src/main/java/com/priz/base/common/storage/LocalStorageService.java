package com.priz.base.common.storage;

import com.priz.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class LocalStorageService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
            log.info("File storage initialized at: {}", rootLocation);
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not initialize storage directory");
        }
    }

    public String store(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String storedName = UUID.randomUUID().toString() + extension;
        Path targetLocation = rootLocation.resolve(storedName);

        try {
            Files.copy(file.getInputStream(), targetLocation,
                    StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file: {} as {}", originalFilename, storedName);
            return storedName;
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store file: " + originalFilename);
        }
    }

    public Resource loadAsResource(String storedName) {
        try {
            Path filePath = rootLocation.resolve(storedName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new BusinessException(HttpStatus.NOT_FOUND,
                    "File not found: " + storedName);
        } catch (MalformedURLException e) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                    "File not found: " + storedName);
        }
    }

    public void delete(String storedName) {
        try {
            Path filePath = rootLocation.resolve(storedName).normalize();
            Files.deleteIfExists(filePath);
            log.info("Deleted file: {}", storedName);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", storedName, e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete file: " + storedName);
        }
    }

    public Path getRootLocation() {
        return rootLocation;
    }
}
