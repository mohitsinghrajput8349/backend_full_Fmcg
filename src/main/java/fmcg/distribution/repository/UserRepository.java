package fmcg.distribution.repository;

import fmcg.distribution.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
import fmcg.distribution.enums.Role;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByRoleAndIsActiveTrue(Role role);
    Boolean existsByEmail(String email);
}