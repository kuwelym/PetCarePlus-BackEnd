package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import petitus.petcareplus.dto.response.service.TopProviderServiceResponse;
import petitus.petcareplus.dto.response.user.RecentUserResponse;
import petitus.petcareplus.service.StatisticService;

import java.util.List;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
@Tag(name = "Provider Dashboard", description = "APIs cho provider dashboard")
@SecurityRequirement(name = "bearerAuth")

public class StatisticController {

    private final StatisticService statisticService;

    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    @GetMapping("/recent-users")
    @Operation(summary = "Get top 5 recent users", description = "Lấy top 5 người dùng gần nhất đã đặt booking")
    public ResponseEntity<List<RecentUserResponse>> getRecentUsers() {
        List<RecentUserResponse> recentUsers = statisticService.getMyTop5RecentUsers();
        return ResponseEntity.ok(recentUsers);
    }

    @GetMapping("/top-provider-services")
    @Operation(summary = "Get top 5 most booked provider services", description = "Lấy top 5 provider-service được booking nhiều nhất")
    public ResponseEntity<List<TopProviderServiceResponse>> getTop5ProviderServices() {
        List<TopProviderServiceResponse> topServices = statisticService.getTop5ProviderServices();
        return ResponseEntity.ok(topServices);
    }

    // @GetMapping("/top-provider-services/period")
    // @Operation(summary = "Get top 5 most booked provider services in period",
    // description = "Lấy top 5 provider-service được booking nhiều nhất trong
    // khoảng thời gian")
    // public ResponseEntity<List<TopProviderServiceResponse>>
    // getTop5ProviderServicesInPeriod(
    // @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    // LocalDateTime startDate,
    // @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    // LocalDateTime endDate) {

    // List<TopProviderServiceResponse> topServices =
    // statisticService.getTop5ProviderServicesInPeriod(startDate,
    // endDate);
    // return ResponseEntity.ok(topServices);
    // }

    @GetMapping("/top-provider-services/custom")
    @Operation(summary = "Get top N most booked provider services", description = "Lấy top N provider-service được booking nhiều nhất")
    public ResponseEntity<List<TopProviderServiceResponse>> getTopProviderServices(
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit) {

        List<TopProviderServiceResponse> topServices = statisticService.getTopProviderServices(limit);
        return ResponseEntity.ok(topServices);
    }

    // Alternative: Admin có thể xem với filters khác
    // @PreAuthorize("hasAuthority('ADMIN')")
    // @GetMapping("/admin/top-provider-services")
    // @Operation(summary = "Admin - Get top provider services with advanced
    // filters")
    // public ResponseEntity<List<TopProviderServiceResponse>>
    // getTopProviderServicesForAdmin(
    // @RequestParam(defaultValue = "5") int limit,
    // @RequestParam(required = false) @DateTimeFormat(iso =
    // DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
    // @RequestParam(required = false) @DateTimeFormat(iso =
    // DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

    // List<TopProviderServiceResponse> topServices;

    // if (startDate != null && endDate != null) {
    // topServices = statisticService.getTop5ProviderServicesInPeriod(startDate,
    // endDate);
    // } else {
    // topServices = statisticService.getTopProviderServices(limit);
    // }

    // return ResponseEntity.ok(topServices);
    // }

}