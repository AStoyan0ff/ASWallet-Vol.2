package STARTER.Services.Impl;

import STARTER.DTOs.SystemAnnouncementViewDTO;
import STARTER.Models.SystemAnnouncement;
import STARTER.Repositories.SystemAnnouncementRepository;
import STARTER.Services.Interface.SystemAnnouncementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class SystemAnnouncementServiceImpl implements SystemAnnouncementService {

    private static final ZoneId APP_ZONE = ZoneId.of("Europe/Sofia");
    private final SystemAnnouncementRepository announcementRepository;

    public SystemAnnouncementServiceImpl(SystemAnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SystemAnnouncementViewDTO> getActiveAnnouncement() {

        return announcementRepository.findFirstByActiveTrueOrderByUpdatedAtDesc()
                .map(this::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SystemAnnouncementViewDTO> getLatestAnnouncement() {

        return announcementRepository.findFirstByOrderByUpdatedAtDesc()
                .map(this::toView);
    }

    @Override
    @Transactional
    public SystemAnnouncementViewDTO publish(String message, String adminUsername) {

        String trimmed = message == null
            ? ""
            : message.trim();

        SystemAnnouncement announcement = announcementRepository.findFirstByOrderByUpdatedAtDesc()
                .orElseGet(SystemAnnouncement::new);

        announcement.setMessage(trimmed);
        announcement.setActive(true);
        announcement.setUpdatedByUsername(adminUsername);
        announcement.setUpdatedAt(LocalDateTime.now(APP_ZONE));

        return toView(announcementRepository.save(announcement));
    }

    @Override
    @Transactional
    public void deactivate(String adminUsername) {

        SystemAnnouncement announcement = announcementRepository.findFirstByOrderByUpdatedAtDesc()
                .orElse(null);

        if (announcement == null) {
            return;
        }

        announcement.setActive(false);
        announcement.setUpdatedByUsername(adminUsername);
        announcement.setUpdatedAt(LocalDateTime.now(APP_ZONE));

        announcementRepository.save(announcement);
    }

    @Override
    @Transactional
    public int clearAll() {

        long count = announcementRepository.count();

        if (count == 0) {
            return 0;
        }

        announcementRepository.deleteAll();
        return (int) count;
    }

    private SystemAnnouncementViewDTO toView(SystemAnnouncement announcement) {

        return SystemAnnouncementViewDTO.builder()
                .message(announcement.getMessage())
                .active(announcement.isActive())
                .updatedByUsername(announcement.getUpdatedByUsername())
                .updatedAt(announcement.getUpdatedAt())
                .build();
    }
}
