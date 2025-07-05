package petitus.petcareplus.dto.response.profile;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import petitus.petcareplus.dto.response.user.UserResponse;
import petitus.petcareplus.model.profile.Profile;
import petitus.petcareplus.model.profile.ServiceProviderProfile;

import java.util.Set;

@Getter
@Setter
@SuperBuilder
public class ServiceProviderProfileResponse extends ProfileResponse {
    private String contactPhone;

    private String contactEmail;

    private double rating;

    private Set<String> skills;

    private Set<String> imageUrls;

    public static ServiceProviderProfileResponse convert(ServiceProviderProfile serviceProviderProfile) {
        Profile profile = serviceProviderProfile.getProfile();
        ServiceProviderProfileResponse.ServiceProviderProfileResponseBuilder<?, ?> builder = ServiceProviderProfileResponse
                .builder()
                .id(profile.getId().toString())
                .user(UserResponse.convert(profile.getUser()))
                .dob(profile.getDob())
                .gender(profile.getGender())
                .avatarUrl(profile.getAvatarUrl())
                .location(profile.getLocation())
                .about(profile.getAbout())
                .isServiceProvider(profile.isServiceProvider())
                .contactPhone(serviceProviderProfile.getContactPhone())
                .contactEmail(serviceProviderProfile.getContactEmail())
                .rating(serviceProviderProfile.getRating())
                .skills(serviceProviderProfile.getSkills())
                .imageUrls(serviceProviderProfile.getImageUrls())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .deletedAt(profile.getDeletedAt());

        return builder.build();
    }
}
