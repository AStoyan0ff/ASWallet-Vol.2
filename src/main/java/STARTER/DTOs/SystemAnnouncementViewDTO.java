package STARTER.DTOs;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SystemAnnouncementViewDTO {

    private final String message;
    private final boolean active;
    private final String updatedByUsername;
    private final LocalDateTime updatedAt;
}
