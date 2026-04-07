package fmcg.distribution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import fmcg.distribution.entity.User;
import fmcg.distribution.repository.UserRepository;
import fmcg.distribution.enums.Role;

@SpringBootApplication
public class FmcgDistributionApplication {

    public static void main(String[] args) {
        SpringApplication.run(FmcgDistributionApplication.class, args);
    }

    @Bean
    CommandLineRunner init(@Autowired UserRepository userRepository, 
                          @Autowired PasswordEncoder passwordEncoder) {
        return args -> {
            // Create default admin if not exists
            if (userRepository.findByEmail("admin@fmcg.com").isEmpty()) {
                User admin = new User();
                admin.setEmail("admin@fmcg.com");
                admin.setName("Admin User");
                admin.setPhone("9999999999");
                admin.setPassword(passwordEncoder.encode("Admin@123"));
                admin.setRole(Role.ADMIN);
                admin.setCreditPeriodDays(7);
                admin.setIsActive(true);
                userRepository.save(admin);
                System.out.println("Default admin created: admin@fmcg.com / Admin@123");
            }
        };
    }
}