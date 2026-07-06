package STARTER.Repositories;

import STARTER.Models.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository <Wallet, UUID> {

    // Optional<Wallet> findByUser(User user);
    Optional<Wallet> findByUser_Id(UUID userId);

    // List<Wallet> findAllByBalance(BigDecimal balance);
}
