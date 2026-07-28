package STARTER.Services.Interface;

import STARTER.DTOs.SystemAnnouncementViewDTO;
import java.util.Optional;

public interface SystemAnnouncementService {

    Optional<SystemAnnouncementViewDTO> getActiveAnnouncement();
    Optional<SystemAnnouncementViewDTO> getLatestAnnouncement();
    SystemAnnouncementViewDTO publish(String message, String adminUsername);

    void deactivate(String adminUsername);
    int clearAll();
}
