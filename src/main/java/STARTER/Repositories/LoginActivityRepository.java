package STARTER.Repositories;

import STARTER.Models.LoginActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoginActivityRepository extends JpaRepository<LoginActivity, UUID> {

    List<LoginActivity> findAllByOrderByLoggedInAtDesc(Pageable pageable);
}
