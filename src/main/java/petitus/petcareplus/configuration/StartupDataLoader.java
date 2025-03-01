package petitus.petcareplus.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import petitus.petcareplus.model.Role;
import petitus.petcareplus.repository.RoleRepository;
import petitus.petcareplus.utils.Constants;

@Configuration
@RequiredArgsConstructor
public class StartupDataLoader {

    private final RoleRepository roleRepository;

    @Bean
    public CommandLineRunner initializeRoles() {
        return args -> {
            if (roleRepository.findByName(Constants.RoleEnum.ADMIN).isEmpty()) {
                roleRepository.save(new Role(Constants.RoleEnum.ADMIN));
            }
            if (roleRepository.findByName(Constants.RoleEnum.USER).isEmpty()) {
                roleRepository.save(new Role(Constants.RoleEnum.USER));
            }
            if (roleRepository.findByName(Constants.RoleEnum.SERVICE_PROVIDER).isEmpty()) {
                roleRepository.save(new Role(Constants.RoleEnum.SERVICE_PROVIDER));
            }
        };
    }
}
