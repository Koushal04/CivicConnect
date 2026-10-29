
package com.civicconnect.api.entity;

import com.civicconnect.api.enums.UserRole;
import com.civicconnect.api.repository.DepartmentRepository;
import com.civicconnect.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("mysql")
@ConditionalOnProperty(
    name = "app.bootstrap-demo.enabled",
    havingValue = "true"
)
public class DemoAccountBootstrap implements CommandLineRunner {

    private final DepartmentRepository departments;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Value("${CIVIC_DEMO_PASSWORD}")
    private String demoPassword;

    public DemoAccountBootstrap(
            DepartmentRepository departments,
            UserRepository users,
            PasswordEncoder encoder) {
        this.departments = departments;
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Department department = departments.findByCode("PUBLIC_WORKS")
            .orElseGet(() -> {
                Department d = new Department();
                d.setCode("PUBLIC_WORKS");
                d.setName("Public Works");
                d.setDescription("Roads and public infrastructure grievances");
                d.setActive(true);
                return departments.save(d);
            });

        createAccount(
            "CivicConnect Demo Admin",
            "admin.demo@civicconnect.local",
            UserRole.ADMIN,
            null
        );

        createAccount(
            "CivicConnect Demo Officer",
            "officer.demo@civicconnect.local",
            UserRole.OFFICER,
            department
        );

        System.out.println("Demo account bootstrap completed.");
    }

    private void createAccount(
            String name,
            String email,
            UserRole role,
            Department department) {

        var existing = users.findByEmail(email);

        if (existing.isPresent()) {
            User user = existing.get();

            if (user.getRole() != role) {
                throw new IllegalStateException(
                    "Existing account has an unexpected role: " + email
                );
            }

            if (role == UserRole.OFFICER
                    && (user.getDepartment() == null
                    || !user.getDepartment().getId()
                        .equals(department.getId()))) {
                throw new IllegalStateException(
                    "Existing officer belongs to another department."
                );
            }

            System.out.println("Existing account preserved: " + email);
            return;
        }

        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(demoPassword));
        user.setRole(role);
        user.setDepartment(department);
        user.setEnabled(true);

        users.save(user);
        System.out.println("Created " + role + ": " + email);
    }
}
