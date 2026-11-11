package com.projectmgmttool.backend.config;

import com.projectmgmttool.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The demo seeder is opt-in via app.seed-demo-data. With the flag unset (the
 * default, and what the test profile uses) it must not even exist as a bean.
 */
@SpringBootTest
@ActiveProfiles("test")
class DemoDataSeederDisabledTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Test
    void flagOff_seederBeanIsNotCreated() {
        assertEquals(0, context.getBeanNamesForType(DemoDataSeeder.class).length);
    }

    @Test
    void flagOff_noDemoAccountsExist() {
        assertFalse(userRepository.existsByEmail("demo-admin@taskforge.dev"));
        assertEquals(0, userRepository.count());
    }
}
