package fmcg.distribution.repository;

import fmcg.distribution.entity.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    Optional<PasswordReset> findByEmailAndResetCode(String email, String resetCode);
    void deleteByEmail(String email);
}