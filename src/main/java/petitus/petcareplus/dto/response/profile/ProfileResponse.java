package petitus.petcareplus.dto.response.profile;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import petitus.petcareplus.dto.response.ResponseObject;
import petitus.petcareplus.model.profile.Profile;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
public class ProfileResponse extends ResponseObject {
    private String id;

    private String userId;

    private LocalDate dob;

    private String gender;

    private boolean isServiceProvider;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public static ProfileResponse convert(Profile profile) {
        if (profile.isServiceProvider())
            return ServiceProviderProfileResponse.convert(profile.getServiceProviderProfile());
        ProfileResponse.ProfileResponseBuilder<?, ?> builder = ProfileResponse.builder()
                .id(profile.getId().toString())
                .userId(profile.getUser().getId().toString())
                .dob(profile.getDob())
                .gender(profile.getGender())
                .isServiceProvider(profile.isServiceProvider())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .deletedAt(profile.getDeletedAt());

        return builder.build();
    }
}
