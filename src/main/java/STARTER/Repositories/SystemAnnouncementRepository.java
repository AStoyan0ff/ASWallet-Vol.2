package STARTER.Repositories;

import STARTER.Models.SystemAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemAnnouncementRepository extends JpaRepository<SystemAnnouncement, UUID> {

    Optional<SystemAnnouncement> findFirstByOrderByUpdatedAtDesc();
    Optional<SystemAnnouncement> findFirstByActiveTrueOrderByUpdatedAtDesc();
}
