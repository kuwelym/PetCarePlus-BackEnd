package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
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
}