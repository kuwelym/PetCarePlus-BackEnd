package petitus.petcareplus.dto.response.user;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import petitus.petcareplus.dto.response.ResponseObject;
import petitus.petcareplus.model.User;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
public class UserResponse extends ResponseObject {
    private String id;

    private String email;

    private String name;

    private String lastName;

    private String phoneNumber;

    private String role;

    private LocalDateTime emailVerifiedAt;

    private LocalDateTime blockedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;


    /**
     * Convert User to UserResponse
     */
    public static UserResponse convert(User user) {
        return UserResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .name(user.getName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().getName().getValue())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .blockedAt(user.getBlockedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .build();
    }
}
