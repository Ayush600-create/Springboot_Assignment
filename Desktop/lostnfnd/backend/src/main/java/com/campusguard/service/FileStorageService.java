package com.campusguard.service;

import com.campusguard.config.CampusGuardProperties;
import com.campusguard.web.ApiException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private final Path root;

    public FileStorageService(CampusGuardProperties properties) throws IOException {
        this.root = Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!ALLOWED.contains(contentType)) {
            throw ApiException.bad("Photographs must be JPEG, PNG, WebP or GIF");
        }
        if (file.getSize() > 5 * 1024 * 1024L) {
            throw ApiException.bad("Photograph must be 5MB or smaller");
        }
        String ext = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Files.copy(file.getInputStream(), root.resolve(name));
        } catch (IOException e) {
            throw ApiException.bad("Could not store the photograph");
        }
        return name;
    }

    public Path resolve(String filename) {
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw ApiException.notFound("File not found");
        }
        Path path = root.resolve(filename).normalize();
        if (!path.startsWith(root) || !Files.exists(path)) {
            throw ApiException.notFound("File not found");
        }
        return path;
    }

    public MediaType mediaType(String filename) {
        if (filename.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (filename.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (filename.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }
}
