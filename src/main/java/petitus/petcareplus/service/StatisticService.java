package petitus.petcareplus.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import petitus.petcareplus.dto.response.service.TopProviderServiceResponse;
import petitus.petcareplus.dto.response.user.RecentUserResponse;
import petitus.petcareplus.model.ProviderService;
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

    @Transactional(readOnly = true)
    public List<TopProviderServiceResponse> getTop5ProviderServices() {
        PageRequest pageRequest = PageRequest.of(0, 5);
        List<Object[]> results = bookingRepository.findTopProviderServicesByBookingCount(pageRequest);

        return results.stream()
                .map(this::mapToTopProviderServiceResponse)
                .collect(Collectors.toList());
    }

    // @Transactional(readOnly = true)
    // public List<TopProviderServiceResponse> getTop5ProviderServicesInPeriod(
    // LocalDateTime startDate, LocalDateTime endDate) {
    // PageRequest pageRequest = PageRequest.of(0, 5);
    // List<Object[]> results =
    // bookingRepository.findTopProviderServicesByBookingCountInPeriod(
    // startDate, endDate, pageRequest);

    // return results.stream()
    // .map(this::mapToTopProviderServiceResponse)
    // .collect(Collectors.toList());
    // }

    @Transactional(readOnly = true)
    public List<TopProviderServiceResponse> getTopProviderServices(int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        List<Object[]> results = bookingRepository.findTopProviderServicesByBookingCount(pageRequest);

        return results.stream()
                .map(this::mapToTopProviderServiceResponse)
                .collect(Collectors.toList());
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

    private TopProviderServiceResponse mapToTopProviderServiceResponse(Object[] result) {
        ProviderService ps = (ProviderService) result[0];
        Long totalBookings = ((Number) result[1]).longValue();

        return TopProviderServiceResponse.builder()
                .id(ps.getId())
                .providerId(ps.getProvider().getId())
                .providerName(ps.getProvider().getFullName())
                .providerAvatarUrl(
                        ps.getProvider().getProfile() != null ? ps.getProvider().getProfile().getAvatarUrl() : null)
                .serviceId(ps.getService().getId())
                .serviceName(ps.getService().getName())
                .serviceIconUrl(ps.getService().getIconUrl())
                .customPrice(ps.getCustomPrice())
                .basePrice(ps.getService().getBasePrice())
                .customDescription(ps.getCustomDescription())
                .totalBookings(totalBookings)
                .build();
    }

}