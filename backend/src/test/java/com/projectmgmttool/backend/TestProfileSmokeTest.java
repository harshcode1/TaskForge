package com.projectmgmttool.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the test setup itself: integration tests must run on the in-memory
 * H2 database under the "test" profile and never touch a real MySQL.
 */
@SpringBootTest
@ActiveProfiles("test")
class TestProfileSmokeTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Test
    void testProfileUsesInMemoryH2_notMySql() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertTrue(url.startsWith("jdbc:h2:mem:"), "Tests must use in-memory H2, but datasource was: " + url);
        }
    }

    @Test
    void testProfileIsTheOnlyActiveProfile() {
        assertTrue(environment.acceptsProfiles(Profiles.of("test")));
        assertFalse(environment.acceptsProfiles(Profiles.of("dev")), "The dev profile must not be active in tests");
        assertFalse(environment.acceptsProfiles(Profiles.of("prod")));
    }
}
