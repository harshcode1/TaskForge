package com.projectmgmttool.backend.config;

import com.projectmgmttool.backend.entity.Comment;
import com.projectmgmttool.backend.entity.Project;
import com.projectmgmttool.backend.entity.ProjectMember;
import com.projectmgmttool.backend.entity.Task;
import com.projectmgmttool.backend.entity.User;
import com.projectmgmttool.backend.entity.enums.Priority;
import com.projectmgmttool.backend.entity.enums.Role;
import com.projectmgmttool.backend.entity.enums.TaskStatus;
import com.projectmgmttool.backend.repository.CommentRepository;
import com.projectmgmttool.backend.repository.ProjectMemberRepository;
import com.projectmgmttool.backend.repository.ProjectRepository;
import com.projectmgmttool.backend.repository.TaskRepository;
import com.projectmgmttool.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Seeds three fixed demo accounts (one per Role) plus realistic project data,
 * so a "Try as Admin/Manager/Member" button on the landing page can log
 * straight into a populated instance — no signup friction for a recruiter
 * clicking a live demo link.
 *
 * Runs on every startup in every profile (dev today, prod once deployed) and
 * is idempotent: if demo-admin@taskforge.dev already exists, it does nothing.
 * That's a deliberate simplification over a "reset demo data" flow — a real
 * multi-tenant demo product would need one, this doesn't; if a demo user
 * edits the seeded data, it just stays edited until someone truly resets the
 * database.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_PASSWORD = "Demo1234";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectMemberRepository projectMemberRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail("demo-admin@taskforge.dev")) {
            return;
        }
        log.info("Seeding demo accounts and sample data (first run only)...");

        User admin = save(new User("Aria Patel", "demo-admin@taskforge.dev", passwordEncoder.encode(DEMO_PASSWORD), Role.ADMIN));
        User manager = save(new User("Jordan Lee", "demo-manager@taskforge.dev", passwordEncoder.encode(DEMO_PASSWORD), Role.MANAGER));
        User member = save(new User("Sam Rivera", "demo-member@taskforge.dev", passwordEncoder.encode(DEMO_PASSWORD), Role.MEMBER));

        Project website = save(new Project("Website Relaunch", "Redesign and relaunch the marketing site ahead of Q4.", admin));
        save(new ProjectMember(manager, website, Role.MANAGER));
        save(new ProjectMember(member, website, Role.MEMBER));

        Project mobile = save(new Project("Mobile App v2", "Second major version of the companion mobile app.", admin));
        save(new ProjectMember(manager, mobile, Role.MANAGER));

        Project internal = save(new Project("Internal Tools", "Dashboards and scripts the team uses day to day.", manager));
        save(new ProjectMember(member, internal, Role.MEMBER));

        LocalDate today = LocalDate.now();

        Task t1 = task(website, "Design new landing page", "Refresh hero, feature grid, and CTA sections.", TaskStatus.TODO, Priority.MEDIUM, manager, today.plusDays(14));
        Task t2 = task(website, "Fix login race condition", "Two rapid submits can create duplicate sessions.", TaskStatus.TODO, Priority.HIGH, member, today.plusDays(4));
        task(website, "Write onboarding docs", "Getting-started guide for new signups.", TaskStatus.TODO, Priority.LOW, null, null);
        task(website, "Migrate hero images to CDN", "Move static assets off the app server.", TaskStatus.IN_PROGRESS, Priority.MEDIUM, member, today.plusDays(9));
        Task t5 = task(website, "Set up analytics events", "Track signup funnel drop-off points.", TaskStatus.IN_PROGRESS, Priority.HIGH, manager, today.plusDays(6));
        task(website, "Awaiting design review", "Blocked on brand team sign-off.", TaskStatus.PENDING, Priority.MEDIUM, admin, today.minusDays(1));
        task(website, "Ship responsive nav", "Mobile hamburger menu and an accessibility pass.", TaskStatus.DONE, Priority.MEDIUM, manager, today.minusDays(7));
        task(website, "Set up staging environment", "Mirror prod config for QA.", TaskStatus.DONE, Priority.HIGH, admin, today.minusDays(12));

        Task t9 = task(mobile, "Offline mode sync conflicts", "Resolve last-write-wins edge cases.", TaskStatus.TODO, Priority.HIGH, manager, today.minusDays(3));
        task(mobile, "Push notification opt-in flow", "iOS 18 permission prompt timing.", TaskStatus.TODO, Priority.MEDIUM, null, null);
        task(mobile, "Dark mode QA pass", "Check contrast across all screens.", TaskStatus.IN_PROGRESS, Priority.LOW, manager, today.plusDays(11));
        task(mobile, "App store screenshots", "Update for the new nav.", TaskStatus.DONE, Priority.LOW, admin, today.minusDays(15));

        task(internal, "Automate weekly usage report", "Currently a manual spreadsheet pull.", TaskStatus.TODO, Priority.MEDIUM, member, today.plusDays(20));
        task(internal, "Add dark mode to admin panel", "Match the main app's theme.", TaskStatus.PENDING, Priority.LOW, member, null);

        save(new Comment("I can take the race-condition fix today, it's blocking QA.", member, t2));
        save(new Comment("Go ahead — flag me for review before merging.", admin, t2));
        save(new Comment("Landing page copy is with marketing, ETA Thursday.", manager, t1));
        save(new Comment("Events are wired for signup + first-project-created, adding activation next.", manager, t5));
        save(new Comment("Seeing this on iOS only, repro steps in the linked doc.", manager, t9));

        log.info("Demo data seeded: 3 users, 3 projects, 14 tasks, 5 comments.");
    }

    private Task task(Project project, String title, String description, TaskStatus status, Priority priority, User assignee, LocalDate dueDate) {
        Task task = new Task(title, description, project);
        task.setStatus(status);
        task.setPriority(priority);
        task.setAssignee(assignee);
        task.setDueDate(dueDate);
        return save(task);
    }

    private <T> T save(T entity) {
        if (entity instanceof User u) return (T) userRepository.save(u);
        if (entity instanceof Project p) return (T) projectRepository.save(p);
        if (entity instanceof ProjectMember pm) return (T) projectMemberRepository.save(pm);
        if (entity instanceof Task t) return (T) taskRepository.save(t);
        if (entity instanceof Comment c) return (T) commentRepository.save(c);
        throw new IllegalArgumentException("Unhandled entity type: " + entity.getClass());
    }
}
