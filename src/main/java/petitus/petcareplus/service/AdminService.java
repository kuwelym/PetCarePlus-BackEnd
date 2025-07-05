package petitus.petcareplus.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import petitus.petcareplus.dto.request.auth.CreateAdminRequest;
import petitus.petcareplus.exceptions.BadRequestException;
import petitus.petcareplus.model.Role;
import petitus.petcareplus.model.User;
import petitus.petcareplus.repository.UserRepository;
import petitus.petcareplus.utils.Constants;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final MessageSourceService messageSourceService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User changeUserRole(String userId, String roleName) {
        User user = userService.findById(userId);

        // Validate role
        Constants.RoleEnum roleEnum;
        try {
            roleEnum = Constants.RoleEnum.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(messageSourceService.get("invalid_role", new String[] { roleName }));
        }

        Role role = roleService.findByName(roleEnum);
        user.setRole(role);

        return userRepository.save(user);
    }

    @Transactional
    public User createAdmin(CreateAdminRequest request) throws BindException {
        // Check if email already exists
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "request");

        userRepository.findByEmail(request.getEmail())
                .ifPresent(user -> bindingResult.addError(new FieldError(bindingResult.getObjectName(), "email",
                        messageSourceService.get("unique_email"))));

        if (bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }
        // Create new admin user
        Role adminRole = roleService.findByName(Constants.RoleEnum.ADMIN);

        User admin = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .lastName(request.getLastName())
                .role(adminRole)
                .emailVerifiedAt(LocalDateTime.now()) // Admin accounts are pre-verified
                .build();

        return userRepository.save(admin);
    }

    @Transactional
    public User toggleUserBlockStatus(String userId, boolean blocked) {
        User user = userService.findById(userId);

        if (blocked) {
            user.setBlockedAt(LocalDateTime.now());
        } else {
            user.setBlockedAt(null);
        }

        return userRepository.save(user);
    }
}
