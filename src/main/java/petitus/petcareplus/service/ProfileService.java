package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.dto.request.profile.ProfileRequest;

import petitus.petcareplus.model.User;
import petitus.petcareplus.model.profile.Profile;
import petitus.petcareplus.model.spec.ProfileFilterSpecification;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.ProfileCriteria;
import petitus.petcareplus.repository.ProfileRepository;

import petitus.petcareplus.repository.UserRepository;
import petitus.petcareplus.utils.PageRequestBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MessageSourceService messageSourceService;

    public Page<Profile> findAll(ProfileCriteria criteria, PaginationCriteria paginationCriteria) {
        return profileRepository.findAll(new ProfileFilterSpecification(criteria),
                PageRequestBuilder.build(paginationCriteria));
    }

    public Profile getMyProfile() {
        UUID userId = userService.getCurrentUserId();
        return profileRepository.findByUserId(userId);
    }

    public Profile findById(UUID id) {
        return profileRepository.findById(id).orElse(null);
    }

    public Profile findByUserId(UUID userId) {
        return profileRepository.findByUserId(userId);
    }

    @Transactional
    public void updateProfile(ProfileRequest profileRequest) {
        User user = userService.getUser();
        Profile existingProfile = profileRepository.findByUserId(user.getId());

        if (existingProfile == null) {
            throw new RuntimeException(messageSourceService.get("profile_not_found"));
        }

        // Update User entity fields if provided
        if (profileRequest.getName() != null) {
            user.setName(profileRequest.getName());
        }
        if (profileRequest.getLastName() != null) {
            user.setLastName(profileRequest.getLastName());
        }
        if (profileRequest.getPhoneNumber() != null) {
            user.setPhoneNumber(profileRequest.getPhoneNumber());
        }

        // Update Profile entity fields
        existingProfile.setGender(profileRequest.getGender());
        existingProfile.setAvatarUrl(profileRequest.getAvatarUrl());
        existingProfile.setDob(LocalDate.parse(profileRequest.getDob()));
        existingProfile.setLocation(profileRequest.getLocation());
        existingProfile.setAbout(profileRequest.getAbout());

        // Save both User and Profile entities
        userRepository.save(user);
        profileRepository.save(existingProfile);
    }



    @Transactional
    public void createDefaultProfile(User user) {
        Profile defaultProfile = Profile.builder()
                .user(user)
                .isServiceProvider(false)
                .build();

        profileRepository.save(defaultProfile);
    }
}
