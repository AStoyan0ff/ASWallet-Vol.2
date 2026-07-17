package STARTER.Repositories;

import STARTER.Enums.AccountStatus;
import STARTER.Models.UserProfileDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

// Advanced: user profile details repository
@Repository
public interface UserProfileDetailsRepository extends JpaRepository<UserProfileDetails, UUID> {

    Optional<UserProfileDetails> findByUser_Username(String username);
    Optional<UserProfileDetails> findByUser_Id(UUID userId);

    void deleteByUser_Id(UUID userId);
    long countByAccountStatus(AccountStatus accountStatus);
}


