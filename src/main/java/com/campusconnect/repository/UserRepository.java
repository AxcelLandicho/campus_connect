package com.campusconnect.repository;

import com.campusconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM APP_USERS WHERE email = :email", nativeQuery = true)
    int deleteByEmail(@Param("email") String email);
}
