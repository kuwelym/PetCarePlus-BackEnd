package petitus.petcareplus.dto.response.profile;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import petitus.petcareplus.model.profile.Profile;
import petitus.petcareplus.model.profile.ServiceProviderProfile;

import java.util.Set;

@Getter
@Setter
@SuperBuilder
public class ServiceProviderProfileResponse extends ProfileResponse{
    private String about;

    private String contactPhone;

    private String contactEmail;

    private String location;

    private double rating;

    private Set<String> skills;

    private Set<String> imageUrls;

    public static ServiceProviderProfileResponse convert(ServiceProviderProfile serviceProviderProfile) {
        Profile profile = serviceProviderProfile.getProfile();
        ServiceProviderProfileResponse.ServiceProviderProfileResponseBuilder<?, ?> builder = ServiceProviderProfileResponse.builder()
                .id(profile.getId().toString())
                .userId(profile.getUser().getId().toString())
                .dob(profile.getDob())
                .gender(profile.getGender())
                .isServiceProvider(profile.isServiceProvider())
                .about(serviceProviderProfile.getAbout())
                .contactPhone(serviceProviderProfile.getContactPhone())
                .contactEmail(serviceProviderProfile.getContactEmail())
                .location(serviceProviderProfile.getLocation())
                .rating(serviceProviderProfile.getRating())
                .skills(serviceProviderProfile.getSkills())
                .imageUrls(serviceProviderProfile.getImageUrls());

        return builder.build();
    }
}
