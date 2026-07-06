package STARTER.Repositories;

import STARTER.Models.BankCard;
import STARTER.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankCardRepository extends JpaRepository<BankCard, UUID> {

    Optional<BankCard> findByUser_Username(String username);
    Optional<BankCard> findByUser_Id(UUID userId);

    boolean existsByIban(String iban);
    void deleteByUser(User user);
}
