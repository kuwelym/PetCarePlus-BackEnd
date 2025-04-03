package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.dto.request.review.ServiceReviewRequest;
import petitus.petcareplus.dto.request.review.ServiceReviewUpdateRequest;
import petitus.petcareplus.dto.response.review.ServiceReviewResponse;
import petitus.petcareplus.security.jwt.JwtUserDetails;
import petitus.petcareplus.service.ServiceReviewService;

import java.util.UUID;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Service Reviews", description = "APIs for managing service reviews")
public class ServiceReviewController {

    private final ServiceReviewService serviceReviewService;

    @PostMapping
    @PreAuthorize("hasAuthority('USER')")
    @Operation(summary = "Create a new review", description = "Create a review for a completed service")
    public ResponseEntity<ServiceReviewResponse> createReview(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @Valid @RequestBody ServiceReviewRequest request) {

        ServiceReviewResponse response = serviceReviewService.createReview(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{reviewId}")
    @Operation(summary = "Get review details", description = "Get details of a specific review")
    public ResponseEntity<ServiceReviewResponse> getReview(@PathVariable UUID reviewId) {
        ServiceReviewResponse response = serviceReviewService.getReview(reviewId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{reviewId}")
    @PreAuthorize("hasAuthority('USER')")
    @Operation(summary = "Update a review", description = "Update an existing review")
    public ResponseEntity<ServiceReviewResponse> updateReview(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @PathVariable UUID reviewId,
            @Valid @RequestBody ServiceReviewUpdateRequest request) {

        ServiceReviewResponse response = serviceReviewService.updateReview(userDetails.getId(), reviewId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    @Operation(summary = "Delete a review", description = "Soft-delete a review")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @PathVariable UUID reviewId) {

        serviceReviewService.deleteReview(userDetails.getId(), reviewId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user")
    @PreAuthorize("hasAuthority('USER')")
    @Operation(summary = "Get user's reviews", description = "Get all reviews created by the current user")
    public ResponseEntity<Page<ServiceReviewResponse>> getUserReviews(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String direction,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy) {

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<ServiceReviewResponse> reviews = serviceReviewService.getUserReviews(userDetails.getId(), pageRequest);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/provider-services/{providerServiceId}")
    @Operation(summary = "Get reviews for a provider service", description = "Get reviews for a specific provider service")
    public ResponseEntity<Page<ServiceReviewResponse>> getProviderServiceReviews(
            @PathVariable UUID providerServiceId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String direction,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy) {

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        return ResponseEntity.ok(serviceReviewService.getServiceReviews(providerServiceId, pageRequest));
    }

    @GetMapping("/provider/{providerId}")
    @Operation(summary = "Get provider reviews", description = "Get all reviews for a specific service provider")
    public ResponseEntity<Page<ServiceReviewResponse>> getProviderReviews(
            @PathVariable UUID providerId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String direction,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy) {

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<ServiceReviewResponse> reviews = serviceReviewService.getProviderReviews(providerId, pageRequest);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/provider/{providerId}/rating")
    @Operation(summary = "Get provider average rating", description = "Get the average rating for a service provider")
    public ResponseEntity<Double> getProviderAverageRating(@PathVariable UUID providerId) {
        Double averageRating = serviceReviewService.getProviderAverageRating(providerId);
        return ResponseEntity.ok(averageRating);
    }
}