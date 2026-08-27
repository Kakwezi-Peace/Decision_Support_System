package rw.ac.dss.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.dss.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByResetToken(String resetToken);

    boolean existsByRole(User.Role role);
}
