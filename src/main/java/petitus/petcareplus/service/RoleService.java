package petitus.petcareplus.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.Role;
import petitus.petcareplus.repository.RoleRepository;
import petitus.petcareplus.utils.Constants;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    private final MessageSourceService messageSourceService;

    public Role findByName(final Constants.RoleEnum name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("role_not_found")));
    }

    public Role create(final Role role) {
        return roleRepository.save(role);
    }

    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    public List<Role> saveList(List<Role> roleList) {
        return roleRepository.saveAll(roleList);
    }

    @PostConstruct
    public void initializeRoles() {
        if (roleRepository.findByName(Constants.RoleEnum.ADMIN).isEmpty()) {
            roleRepository.save(new Role(Constants.RoleEnum.ADMIN));
        }
        if (roleRepository.findByName(Constants.RoleEnum.USER).isEmpty()) {
            roleRepository.save(new Role(Constants.RoleEnum.USER));
        }
    }
}