package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.dto.request.profile.ServiceProviderProfileRequest;
import petitus.petcareplus.exceptions.DataExistedException;
import petitus.petcareplus.model.spec.ServiceProviderProfileFilterSpecification;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.ServiceProviderProfileCriteria;
import petitus.petcareplus.model.User;
import petitus.petcareplus.model.profile.Profile;
import petitus.petcareplus.model.profile.ServiceProviderProfile;
import petitus.petcareplus.repository.ProfileRepository;
import petitus.petcareplus.repository.ServiceProviderProfileRepository;
import petitus.petcareplus.repository.UserRepository;
import petitus.petcareplus.utils.Constants;
import petitus.petcareplus.utils.PageRequestBuilder;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceProviderProfileService {
    private final ServiceProviderProfileRepository serviceProviderProfileRepository;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final RoleService roleService;
    private final MessageSourceService messageSourceService;

    public Page<ServiceProviderProfile> findAll(ServiceProviderProfileCriteria criteria, PaginationCriteria paginationCriteria) {
        return serviceProviderProfileRepository.findAll(new ServiceProviderProfileFilterSpecification(criteria),
                PageRequestBuilder.build(paginationCriteria));
    }

    public ServiceProviderProfile findById(UUID id) {
        return serviceProviderProfileRepository.findById(id).orElse(null);
    }

    public ServiceProviderProfile findByProfileId(UUID profileId) {
        return serviceProviderProfileRepository.findByProfileId(profileId);
    }

    public ServiceProviderProfile getMyServiceProviderProfile() {
        UUID userId = userService.getCurrentUserId();
        Profile profile = profileRepository.findByUserId(userId);
        if (profile != null && profile.isServiceProvider()) {
            return profile.getServiceProviderProfile();
        }
        return null;
    }

    public boolean hasServiceProviderProfile() {
        UUID userId = userService.getCurrentUserId();
        Profile profile = profileRepository.findByUserId(userId);
        return profile != null && profile.isServiceProvider() && profile.getServiceProviderProfile() != null;
    }

    private void validateServiceProfileExists(UUID profileId) {
        if (serviceProviderProfileRepository.findByProfileId(profileId) != null) {
            throw new DataExistedException(messageSourceService.get("profile_exists"));
        }
    }

    private void setupBidirectionalRelationship(Profile profile, ServiceProviderProfile serviceProviderProfile) {
        profile.setServiceProvider(true);
        profile.setServiceProviderProfile(serviceProviderProfile);
        serviceProviderProfile.setProfile(profile);
    }

    @Transactional
    public void saveServiceProviderProfile(ServiceProviderProfileRequest serviceProviderProfileRequest) {
        User user = userService.getUser();
        Profile existingProfile = profileRepository.findByUserId(user.getId());
        
        if (existingProfile == null) {
            throw new RuntimeException(messageSourceService.get("profile_not_found"));
        }
        
        // Check if user already has a service provider profile
        if (existingProfile.isServiceProvider() && existingProfile.getServiceProviderProfile() != null) {
            throw new DataExistedException(messageSourceService.get("service_provider_profile_already_exists"));
        }
        
        // Double check using repository method
        validateServiceProfileExists(existingProfile.getId());

        user.setRole(roleService.findByName(Constants.RoleEnum.SERVICE_PROVIDER));

        // Create a new ServiceProviderProfile linked to the existing Profile
        ServiceProviderProfile serviceProviderProfile = ServiceProviderProfile.builder()
                .profile(existingProfile)
                .businessName(serviceProviderProfileRequest.getBusinessName())
                .businessBio(serviceProviderProfileRequest.getBusinessBio())
                .businessAddress(serviceProviderProfileRequest.getBusinessAddress())
                .contactEmail(serviceProviderProfileRequest.getContactEmail())
                .contactPhone(serviceProviderProfileRequest.getContactPhone())
                .availableTime(serviceProviderProfileRequest.getAvailableTime())
                .imageUrls(serviceProviderProfileRequest.getImageUrls())
                .build();

        // Set up the bidirectional relationship properly
        setupBidirectionalRelationship(existingProfile, serviceProviderProfile);

        // Update user role
        user.setRole(roleService.findByName(Constants.RoleEnum.SERVICE_PROVIDER));
        userRepository.save(user);
        
        // Save the profile first (which will cascade to service provider profile)
        profileRepository.save(existingProfile);
    }

    @Transactional
    public void updateServiceProviderProfile(ServiceProviderProfileRequest serviceProviderProfileRequest) {
        User user = userService.getUser();
        Profile existingProfile = profileRepository.findByUserId(user.getId());

        if (existingProfile == null) {
            throw new RuntimeException(messageSourceService.get("profile_not_found"));
        }

        ServiceProviderProfile existingServiceProviderProfile = existingProfile.getServiceProviderProfile();

        if (existingServiceProviderProfile == null) {
            throw new RuntimeException(messageSourceService.get("service_provider_profile_not_found"));
        }

        // Update service provider specific information
        existingServiceProviderProfile.setContactEmail(serviceProviderProfileRequest.getContactEmail());
        existingServiceProviderProfile.setContactPhone(serviceProviderProfileRequest.getContactPhone());
        existingServiceProviderProfile.setAvailableTime(serviceProviderProfileRequest.getAvailableTime());
        existingServiceProviderProfile.setImageUrls(serviceProviderProfileRequest.getImageUrls());
        existingServiceProviderProfile.setBusinessName(serviceProviderProfileRequest.getBusinessName());
        existingServiceProviderProfile.setBusinessBio(serviceProviderProfileRequest.getBusinessBio());
        existingServiceProviderProfile.setBusinessAddress(serviceProviderProfileRequest.getBusinessAddress());

        // Save the service provider profile
        serviceProviderProfileRepository.save(existingServiceProviderProfile);
    }
} 
