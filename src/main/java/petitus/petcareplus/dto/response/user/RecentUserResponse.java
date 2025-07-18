package petitus.petcareplus.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentUserResponse {
    private UUID id;
    private String name;
    private String lastName;
    private String email;
    private String avatarUrl;
    private LocalDateTime lastBookingDate;
    private Long totalBookings;
}