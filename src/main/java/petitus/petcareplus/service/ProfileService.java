package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.dto.request.profile.ProfileRequest;
import petitus.petcareplus.dto.request.profile.ServiceProviderProfileRequest;
import petitus.petcareplus.exceptions.DataExistedException;
import petitus.petcareplus.model.User;
import petitus.petcareplus.model.profile.Profile;
import petitus.petcareplus.model.profile.ServiceProviderProfile;
import petitus.petcareplus.model.spec.ProfileFilterSpecification;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.ProfileCriteria;
import petitus.petcareplus.repository.ProfileRepository;
import petitus.petcareplus.repository.ServiceProviderProfileRepository;
import petitus.petcareplus.repository.UserRepository;
import petitus.petcareplus.utils.Constants;
import petitus.petcareplus.utils.PageRequestBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {
    private final ProfileRepository profileRepository;
    private final ServiceProviderProfileRepository serviceProviderProfileRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final RoleService roleService;
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

    private void validateServiceProfileExists(UUID profileId) {
        if (serviceProviderProfileRepository.findByProfileId(profileId) != null) {
            throw new DataExistedException(messageSourceService.get("profile_exists"));
        }
    }

    @Transactional
    public void createDefaultProfile(User user) {
        Profile defaultProfile = Profile.builder()
                .user(user)
                .isServiceProvider(false)
                .build();

        profileRepository.save(defaultProfile);
    }

    @Transactional
    public void saveServiceProviderProfile(ServiceProviderProfileRequest serviceProviderProfileRequest) {
        User user = userService.getUser();
        Profile existingProfile = findByUserId(user.getId());
        validateServiceProfileExists(existingProfile.getId());

        user.setRole(roleService.findByName(Constants.RoleEnum.SERVICE_PROVIDER));

        // Create a new ServiceProviderProfile linked to the existing Profile
        ServiceProviderProfile serviceProviderProfile = ServiceProviderProfile.builder()
                .profile(existingProfile)
                .contactEmail(serviceProviderProfileRequest.getContactEmail())
                .contactPhone(serviceProviderProfileRequest.getContactPhone())
                .availableTime(serviceProviderProfileRequest.getAvailableTime())
                .imageUrls(serviceProviderProfileRequest.getImageUrls())
                .skills(serviceProviderProfileRequest.getSkills())
                .build();

        existingProfile.setServiceProvider(true);
        existingProfile.setServiceProviderProfile(serviceProviderProfile);

        userRepository.save(user);
        serviceProviderProfileRepository.save(serviceProviderProfile);
    }

    @Transactional
    public void updateServiceProviderProfile(ServiceProviderProfileRequest serviceProviderProfileRequest) {
        User user = userService.getUser();
        Profile existingProfile = findByUserId(user.getId());

        if (existingProfile == null) {
            throw new RuntimeException(messageSourceService.get("profile_not_found"));
        }

        ServiceProviderProfile existingServiceProviderProfile = existingProfile.getServiceProviderProfile();

        if (existingServiceProviderProfile == null) {
            throw new RuntimeException(messageSourceService.get("service_provider_profile_not_found"));
        }

        // Update basic profile information (including location and about)
        existingProfile.setGender(serviceProviderProfileRequest.getGender());
        existingProfile.setDob(LocalDate.parse(serviceProviderProfileRequest.getDob()));
        existingProfile.setAvatarUrl(serviceProviderProfileRequest.getAvatarUrl());
        existingProfile.setLocation(serviceProviderProfileRequest.getLocation());
        existingProfile.setAbout(serviceProviderProfileRequest.getAbout());

        // Update service provider specific information
        existingServiceProviderProfile.setContactEmail(serviceProviderProfileRequest.getContactEmail());
        existingServiceProviderProfile.setContactPhone(serviceProviderProfileRequest.getContactPhone());
        existingServiceProviderProfile.setAvailableTime(serviceProviderProfileRequest.getAvailableTime());
        existingServiceProviderProfile.setImageUrls(serviceProviderProfileRequest.getImageUrls());
        existingServiceProviderProfile.setSkills(serviceProviderProfileRequest.getSkills());

        // Save both the profile and service provider profile
        profileRepository.save(existingProfile);
        serviceProviderProfileRepository.save(existingServiceProviderProfile);
    }

}
