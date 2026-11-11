package com.projectmgmttool.backend.config;

import com.projectmgmttool.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Counterpart to DemoDataSeederDisabledTest: the flag really does switch the seeder on. */
@SpringBootTest(properties = "app.seed-demo-data=true")
@ActiveProfiles("test")
class DemoDataSeederEnabledTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Test
    void flagOn_seederRunsAndCreatesDemoAccounts() {
        assertEquals(1, context.getBeanNamesForType(DemoDataSeeder.class).length);
        assertTrue(userRepository.existsByEmail("demo-admin@taskforge.dev"));
        assertTrue(userRepository.existsByEmail("demo-manager@taskforge.dev"));
        assertTrue(userRepository.existsByEmail("demo-member@taskforge.dev"));
    }
}
