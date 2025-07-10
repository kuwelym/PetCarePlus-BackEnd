package petitus.petcareplus.dto.response.profile;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import petitus.petcareplus.dto.response.ResponseObject;
import petitus.petcareplus.dto.response.user.UserResponse;
import petitus.petcareplus.model.profile.Profile;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
public class ProfileResponse extends ResponseObject {
    private String id;

    private UserResponse user;

    private LocalDate dob;

    private String gender;

    private boolean isServiceProvider;

    private String avatarUrl;

    private String location;

    private String about;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public static ProfileResponse convert(Profile profile) {
        if (profile == null) {
            return null;
        }

        ProfileResponse.ProfileResponseBuilder<?, ?> builder = ProfileResponse.builder()
                .id(profile.getId().toString())
                .user(UserResponse.convert(profile.getUser()))
                .dob(profile.getDob())
                .gender(profile.getGender())
                .avatarUrl(profile.getAvatarUrl())
                .location(profile.getLocation())
                .about(profile.getAbout())
                .isServiceProvider(profile.isServiceProvider())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .deletedAt(profile.getDeletedAt());

        return builder.build();
    }
}
