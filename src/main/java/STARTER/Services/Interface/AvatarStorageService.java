package STARTER.Services.Interface;

import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

// Advanced — local avatar file storage
public interface AvatarStorageService {

    String store(UUID userId, MultipartFile file);
    void deleteLocalAvatar(String avatarUrl);
}
