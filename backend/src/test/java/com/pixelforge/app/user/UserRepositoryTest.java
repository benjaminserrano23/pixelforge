package com.pixelforge.app.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest arranca un slice JPA con H2 en memoria (sin Spring Security).
 * Es rápido y cubre la lógica del repositorio sin tocar Postgres.
 */
@DataJpaTest
class UserRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired TestEntityManager em;

    @Test
    void findByEmail_returns_user_when_exists() {
        em.persistAndFlush(newUser("alice@example.com", UserRole.PLAYER));

        assertThat(userRepository.findByEmail("alice@example.com")).isPresent();
        assertThat(userRepository.findByEmail("not-there@example.com")).isEmpty();
    }

    @Test
    void existsByEmail_returns_true_when_present_false_otherwise() {
        em.persistAndFlush(newUser("bob@example.com", UserRole.DEVELOPER));

        assertThat(userRepository.existsByEmail("bob@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void prePersist_sets_createdAt_when_null() {
        User u = newUser("clara@example.com", UserRole.PLAYER);
        u.setCreatedAt(null); // forzar el camino del @PrePersist

        User saved = em.persistAndFlush(u);

        assertThat(saved.getCreatedAt()).isNotNull();
    }

    private User newUser(String email, UserRole role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$10$dummyhashfortest....................");
        u.setDisplayName("Test User");
        u.setRole(role);
        u.setCreatedAt(Instant.now());
        return u;
    }
}
