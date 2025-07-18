package petitus.petcareplus.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import petitus.petcareplus.dto.response.user.RecentUserResponse;
import petitus.petcareplus.model.User;
import petitus.petcareplus.repository.BookingRepository;

@Service
@RequiredArgsConstructor
public class StatisticService {

    private final BookingRepository bookingRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<RecentUserResponse> getTop5RecentUsers(UUID providerId) {
        PageRequest pageRequest = PageRequest.of(0, 5);
        List<Object[]> results = bookingRepository.findTop5RecentUsersByProviderId(providerId, pageRequest);

        return results.stream()
                .map(result -> {
                    User user = (User) result[0];
                    LocalDateTime lastBookingDate = (LocalDateTime) result[1];
                    Long totalBookings = ((Number) result[2]).longValue();

                    return mapToRecentUserResponse(user, lastBookingDate, totalBookings);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecentUserResponse> getMyTop5RecentUsers() {
        UUID currentProviderId = userService.getCurrentUserId();
        return getTop5RecentUsers(currentProviderId);
    }

    private RecentUserResponse mapToRecentUserResponse(User user, LocalDateTime lastBookingDate, Long totalBookings) {
        return RecentUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .avatarUrl(user.getProfile() != null ? user.getProfile().getAvatarUrl() : null)
                .lastBookingDate(lastBookingDate)
                .totalBookings(totalBookings)
                .build();
    }
}