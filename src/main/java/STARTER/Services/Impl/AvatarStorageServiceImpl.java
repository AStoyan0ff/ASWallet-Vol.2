package STARTER.Services.Impl;

import STARTER.CustomException.InvalidAvatarFileException;
import STARTER.Services.Interface.AvatarStorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
// Advanced — saves uploaded avatars under uploads/avatars/
public class AvatarStorageServiceImpl implements AvatarStorageService {

    private static final String PUBLIC_PREFIX = "/uploads/avatars/";
    private static final long MAX_BYTES = 2 * 1024 * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    @Value("${app.upload.avatars-dir:uploads/avatars}")
    private String avatarsDir;

    @PostConstruct
    public void ensureStorageDirectoryExists() {

        try {
            Files.createDirectories(resolveStoragePath());

        } catch (IOException ex) {
            throw new IllegalStateException("Could not create avatar upload directory", ex);
        }
    }

    @Override
    public String store(UUID userId, MultipartFile file) {
        validate(file);

        String extension = EXTENSIONS.get(file.getContentType());
        String filename = userId + "-" + UUID.randomUUID() + extension;
        Path target = resolveStoragePath().resolve(filename).normalize();

        if (!target.startsWith(resolveStoragePath())) {
            throw new InvalidAvatarFileException("Invalid avatar file name.");
        }

        try {
            Files.copy(file.getInputStream(), target);

        } catch (IOException ex) {
            throw new InvalidAvatarFileException("Could not save avatar file. Please try again.");
        }

        return PUBLIC_PREFIX + filename;
    }

    @Override
    public void deleteLocalAvatar(String avatarUrl) {

        if (avatarUrl == null || !avatarUrl.startsWith(PUBLIC_PREFIX)) {
            return;
        }

        String filename = avatarUrl.substring(PUBLIC_PREFIX.length());

        if (filename.isBlank() || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return;
        }

        try {
            Files.deleteIfExists(resolveStoragePath().resolve(filename));

        } catch (IOException ignored) {
            // Best-effort cleanup; stale files are harmless.
        }
    }

    private void validate(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new InvalidAvatarFileException("Please choose an avatar image to upload.");
        }

        if (file.getSize() > MAX_BYTES) {
            throw new InvalidAvatarFileException("Avatar file is too large. Maximum size is 2 MB.");
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidAvatarFileException("Avatar must be a JPG, PNG, or WebP image.");
        }
    }

    private Path resolveStoragePath() {
        return Paths.get(avatarsDir).toAbsolutePath().normalize();
    }
}
