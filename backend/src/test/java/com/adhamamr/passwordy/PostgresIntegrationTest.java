package com.adhamamr.passwordy;

import com.adhamamr.passwordy.model.User;
import com.adhamamr.passwordy.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence parity check against a real PostgreSQL, so dialect/schema-generation differences
 * from the H2 suite are caught. The rest of the suite still runs on H2 with no Docker; this
 * class is skipped automatically when Docker is unavailable ({@code disabledWithoutDocker}).
 *
 * <p>{@link ServiceConnection} points the application datasource at the started container, so
 * Hibernate creates the schema and runs against Postgres for this test only.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UserRepository userRepository;

    @Test
    void persistsAndReadsBackAgainstRealPostgres() {
        User saved = userRepository.save(new User("pg_user", "pg@example.com", "$argon2$hash"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(userRepository.findByUsername("pg_user")).isPresent();
        assertThat(userRepository.existsByEmail("pg@example.com")).isTrue();
    }
}
