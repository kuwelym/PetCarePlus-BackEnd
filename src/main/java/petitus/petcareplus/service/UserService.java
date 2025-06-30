package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import petitus.petcareplus.dto.request.auth.ResendEmailVerificationRequest;
import petitus.petcareplus.dto.request.auth.UpdateUserRequest;
import petitus.petcareplus.event.UserEmailVerificationSendEvent;
import petitus.petcareplus.exceptions.BadRequestException;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.EmailVerificationToken;
import petitus.petcareplus.model.User;
import petitus.petcareplus.model.spec.UserFilterSpecification;
import petitus.petcareplus.utils.Constants;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.UserCriteria;
import petitus.petcareplus.repository.UserRepository;
import petitus.petcareplus.security.jwt.JwtUserDetails;
import petitus.petcareplus.utils.PageRequestBuilder;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;

    private final MessageSourceService messageSourceService;

    private final RateLimitService rateLimitService;

    private final CipherService cipherService;

    private final EmailVerificationTokenService emailVerificationTokenService;

    private final ApplicationEventPublisher eventPublisher;

    private final RoleService roleService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        messageSourceService.get("user_not_found_with_email", new String[] { username })));

        return JwtUserDetails.build(user);
    }

    public UserDetails loadUserById(String id) {
        User user = userRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new UsernameNotFoundException(
                        messageSourceService.get("user_not_found_with_id", new String[] { id })));

        return JwtUserDetails.build(user);
    }

    public Page<User> findAll(UserCriteria criteria, PaginationCriteria paginationCriteria) {
        return userRepository.findAll(new UserFilterSpecification(criteria),
                PageRequestBuilder.build(paginationCriteria));
    }

    public List<User> findAllByIds(List<UUID> ids) {
        return userRepository.findAllById(ids);
    }

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(
                        messageSourceService.get("user_not_found_with_id", new String[] { id.toString() })));
    }

    public User findById(String id) {
        return userRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new BadRequestException(
                        messageSourceService.get("user_not_found_with_id", new String[] { id })));
    }

    public User update(String id, UpdateUserRequest request) throws BindException {
        User user = findById(id);
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setLastName(request.getLastName());

        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(request.getPassword());
        }

        if (request.getRole() != null) {
            user.setRole(roleService.findByName(Constants.RoleEnum.valueOf(request.getRole().toUpperCase())));
        }

        if (request.getIsEmailVerified() != null) {
            if (request.getIsEmailVerified()) {
                user.setEmailVerifiedAt(LocalDateTime.now());
            } else {
                user.setEmailVerifiedAt(null);
            }
        }

        return updateUser(user, request);
    }

    private User updateUser(User user, UpdateUserRequest request) throws BindException {
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "request");
        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmailAndIdNot(request.getEmail(), user.getId())) {
            bindingResult.addError(new FieldError(bindingResult.getObjectName(), "email",
                    messageSourceService.get("already_exists")));
        }

        boolean isRequiredEmailVerification = false;
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            user.setEmail(request.getEmail());
            isRequiredEmailVerification = true;
        }

        if (StringUtils.hasText(request.getName()) && !request.getName().equals(user.getName())) {
            user.setName(request.getName());
        }

        if (StringUtils.hasText(request.getLastName()) && !request.getLastName().equals(user.getLastName())) {
            user.setLastName(request.getLastName());
        }

        if (bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }

        userRepository.save(user);

        if (isRequiredEmailVerification) {
            emailVerificationEventPublisher(user);
        }

        return user;
    }

    public void resendEmailVerificationMail(ResendEmailVerificationRequest request) {
        if (!rateLimitService.canResendVerification(request.getEmail())) {
            throw new BadRequestException(messageSourceService.get("resend_email_verification_rate_limit"));
        }
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException(messageSourceService.get(
                        "user_not_found_with_email",
                        new String[] { request.getEmail() })));

        if (user.getEmailVerifiedAt() != null) {
            throw new BadRequestException(messageSourceService.get("your_email_already_verified"));
        }

        emailVerificationEventPublisher(user);
    }

    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public User getUser() {
        Authentication authentication = getAuthentication();
        if (authentication.isAuthenticated()) {
            try {
                return findById(((JwtUserDetails) authentication.getPrincipal()).getId());
            } catch (ClassCastException | ResourceNotFoundException e) {
                throw new BadCredentialsException(messageSourceService.get("bad_credentials"));
            }
        } else {
            throw new BadCredentialsException(messageSourceService.get("bad_credentials"));
        }
    }

    public UUID getCurrentUserId() {
        Authentication authentication = getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                return ((JwtUserDetails) authentication.getPrincipal()).getId();
            } catch (ClassCastException | ResourceNotFoundException e) {
                throw new BadCredentialsException(messageSourceService.get("bad_credentials"));
            }
        } else {
            throw new BadCredentialsException(messageSourceService.get("bad_credentials"));
        }
    }

    /**
     * Get user ID from Principal (useful for WebSocket context)
     */
    public UUID getUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            throw new BadCredentialsException(messageSourceService.get("bad_credentials"));
        }
        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException(messageSourceService.get("bad_credentials"));
        }
    }

    @Transactional
    public void verifyEmail(String token) {
        String tokenId = cipherService.decryptForURL(token);

        User user = emailVerificationTokenService.getUserByTokenId(tokenId);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        emailVerificationTokenService.deleteByUserId(user.getId());
    }

    protected void emailVerificationEventPublisher(User user) {
        EmailVerificationToken emailVerificationToken = emailVerificationTokenService.create(user);
        eventPublisher.publishEvent(new UserEmailVerificationSendEvent(this, emailVerificationToken));
    }

    public boolean cancelUnverifiedRegistration(String token) {
        String email = cipherService.decryptForURL(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(messageSourceService.get(
                        "user_not_found_with_email",
                        new String[] { email })));

        // Only delete if the user has NOT verified their email
        if (user.getEmailVerifiedAt() == null) {
            userRepository.delete(user);
            return true;
        }
        return false;
    }

    public boolean hasRole(String roleName) {
        try {
            User currentUser = getUser();
            return currentUser.getRole().getName().name().equals(roleName);
        } catch (Exception e) {
            return false;
        }
    }
}
