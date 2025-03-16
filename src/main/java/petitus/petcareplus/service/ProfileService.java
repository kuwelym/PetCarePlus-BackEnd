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
import petitus.petcareplus.utils.PageRequestBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {
    private final ProfileRepository profileRepository;
    private final ServiceProviderProfileRepository serviceProviderProfileRepository;
    private final UserService userService;
    private final MessageSourceService messageSourceService;

    public Page<Profile> findAll(ProfileCriteria criteria, PaginationCriteria paginationCriteria) {
        return profileRepository.findAll(new ProfileFilterSpecification(criteria),
                PageRequestBuilder.build(paginationCriteria));
    }

    public Profile getMyProfile() {
        User user = userService.getUser();
        return profileRepository.findByUserId(user.getId());
    }

    public Profile findById(String id) {
        return profileRepository.findById(UUID.fromString(id)).orElse(null);
    }

    public Profile findByUserId(String userId) {
        return profileRepository.findByUserId(UUID.fromString(userId));
    }

    @Transactional
    public void saveProfile(ProfileRequest profileRequest) {
        User user = userService.getUser();
        validateProfileExists(user.getId().toString());

        profileRepository.save(Profile.builder()
                .gender(profileRequest.getGender())
                .dob(LocalDate.parse(profileRequest.getDob()))
                .user(user)
                .build());
    }

    private void validateProfileExists(String userId) {
        if (profileRepository.findByUserId(UUID.fromString(userId)) != null) {
            throw new DataExistedException(messageSourceService.get("profile_exists"));
        }
    }

    @Transactional
    public void saveServiceProviderProfile(ServiceProviderProfileRequest serviceProviderProfileRequest) {
        User user = userService.getUser();
        validateProfileExists(user.getId().toString());

        Profile existingProfile = findByUserId(user.getId().toString());

        if (existingProfile == null) {
            // If the user doesn't have a profile, create one first
            existingProfile = Profile.builder()
                    .user(user)
                    .dob(LocalDate.parse(serviceProviderProfileRequest.getDob()))
                    .gender(serviceProviderProfileRequest.getGender())
                    .build();
            existingProfile = profileRepository.save(existingProfile);
        }

        // Create a new ServiceProviderProfile linked to the existing Profile
        ServiceProviderProfile serviceProviderProfile = ServiceProviderProfile.builder()
                .profile(existingProfile)
                .about(serviceProviderProfileRequest.getAbout())
                .contactEmail(serviceProviderProfileRequest.getContactEmail())
                .contactPhone(serviceProviderProfileRequest.getContactPhone())
                .availableTime(serviceProviderProfileRequest.getAvailableTime())
                .imageUrls(serviceProviderProfileRequest.getImageUrls())
                .skills(serviceProviderProfileRequest.getSkills())
                .location(serviceProviderProfileRequest.getLocation())
                .rating(serviceProviderProfileRequest.getRating())
                .build();

        existingProfile.setServiceProvider(true);
        existingProfile.setServiceProviderProfile(serviceProviderProfile);

        serviceProviderProfileRepository.save(serviceProviderProfile);
    }

}
