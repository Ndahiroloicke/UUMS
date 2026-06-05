package com.example.uums.config;

import com.example.uums.entity.User;
import com.example.uums.enums.UserRole;
import com.example.uums.enums.UserStatus;
import com.example.uums.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        createAdminIfNotExists();
    }

    private void createAdminIfNotExists() {
        if (!userRepository.existsByEmail("admin@wasac.rw")) {
            User admin = User.builder()
                    .fullNames("System Administrator")
                    .email("admin@wasac.rw")
                    .phoneNumber("+250788000000")
                    .password(passwordEncoder.encode("Admin@1234"))
                    .role(UserRole.ROLE_ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(admin);
            log.info("Default admin user created: admin@wasac.rw / Admin@1234");
        }
    }
}
