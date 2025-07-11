package petitus.petcareplus.dto.response.profile;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import petitus.petcareplus.model.profile.Profile;
import petitus.petcareplus.model.profile.ServiceProviderProfile;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@SuperBuilder
public class ServiceProviderProfileResponse {
    private String id;

    private String profileId;

    private String businessName;

    private String businessBio;

    private String businessAddress;

    private String contactPhone;

    private String contactEmail;

    private double rating;

    private Map<String, Object> availableTime;

    private Set<String> imageUrls;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public static ServiceProviderProfileResponse convert(ServiceProviderProfile serviceProviderProfile) {
        Profile profile = serviceProviderProfile.getProfile();
        ServiceProviderProfileResponse.ServiceProviderProfileResponseBuilder<?, ?> builder = ServiceProviderProfileResponse
                .builder()
                .profileId(profile.getId().toString())
                .id(serviceProviderProfile.getId().toString())
                .businessName(serviceProviderProfile.getBusinessName())
                .businessBio(serviceProviderProfile.getBusinessBio())
                .businessAddress(serviceProviderProfile.getBusinessAddress())
                .contactPhone(serviceProviderProfile.getContactPhone())
                .contactEmail(serviceProviderProfile.getContactEmail())
                .availableTime(serviceProviderProfile.getAvailableTime())
                .rating(serviceProviderProfile.getRating())
                .imageUrls(serviceProviderProfile.getImageUrls())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .deletedAt(profile.getDeletedAt());

        return builder.build();
    }
}
