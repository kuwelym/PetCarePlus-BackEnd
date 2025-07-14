package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.dto.request.review.ServiceReviewRequest;
import petitus.petcareplus.dto.request.review.ServiceReviewUpdateRequest;
import petitus.petcareplus.dto.response.StandardPaginationResponse;
import petitus.petcareplus.dto.response.review.ServiceReviewResponse;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.ServiceReviewCriteria;
import petitus.petcareplus.service.ServiceReviewService;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Service Reviews", description = "APIs for managing service reviews")
@SecurityRequirement(name = "bearerAuth")
public class ServiceReviewController {

    private final ServiceReviewService serviceReviewService;

    @PostMapping
    @PreAuthorize("hasAuthority('USER')")
    @Operation(summary = "Create a new review", description = "Create a review for a completed service")
    public ResponseEntity<ServiceReviewResponse> createReview(
            @Valid @RequestBody ServiceReviewRequest request) {

        ServiceReviewResponse response = serviceReviewService.createReview(request);
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
            @PathVariable UUID reviewId,
            @Valid @RequestBody ServiceReviewUpdateRequest request) {

        ServiceReviewResponse response = serviceReviewService.updateReview(reviewId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    @Operation(summary = "Delete a review", description = "Soft-delete a review")
    public ResponseEntity<Void> deleteReview(
            @PathVariable UUID reviewId) {

        serviceReviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('USER')")
    @Operation(summary = "Get current user's reviews", description = "Get all reviews created by the current user")
    public ResponseEntity<StandardPaginationResponse<ServiceReviewResponse>> getUserReviews(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sort) {

        PaginationCriteria pagination = PaginationCriteria.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sort(sort)
                .columns(new String[] { "rating", "createdAt", "updatedAt" })
                .build();

        Page<ServiceReviewResponse> reviews = serviceReviewService.getUserReviews(pagination);
        StandardPaginationResponse<ServiceReviewResponse> response = new StandardPaginationResponse<>(
                reviews,
                reviews.getContent());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/provider-services/{providerServiceId}")
    @Operation(summary = "Get reviews for a provider service", description = "Get reviews for a specific provider service")
    public ResponseEntity<StandardPaginationResponse<ServiceReviewResponse>> getProviderServiceReviews(
            @PathVariable UUID providerServiceId,

            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAtStart,
            @RequestParam(required = false) Boolean hasComment,

            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sort) {

        ServiceReviewCriteria criteria = ServiceReviewCriteria.builder()
                .rating(rating)
                .providerServiceId(providerServiceId)
                .minRating(minRating)
                .maxRating(maxRating)
                .createdAtStart(createdAtStart)
                .hasComment(hasComment)
                .build();

        PaginationCriteria pagination = PaginationCriteria.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sort(sort)
                .columns(new String[] { "rating", "createdAt", "updatedAt" })
                .build();

        Page<ServiceReviewResponse> pageResult = serviceReviewService.getServiceReviews(providerServiceId, criteria,
                pagination);

        StandardPaginationResponse<ServiceReviewResponse> response = new StandardPaginationResponse<>(
                pageResult,
                pageResult.getContent());

        return ResponseEntity.ok(response);
    }

    // @GetMapping("/provider/{providerId}")
    // @Operation(summary = "Get provider reviews", description = "Get all reviews
    // for a specific service provider")
    // public ResponseEntity<Page<ServiceReviewResponse>> getProviderReviews(
    // @PathVariable UUID providerId,
    // @RequestParam(defaultValue = "1") Integer page,
    // @RequestParam(defaultValue = "10") Integer size,
    // @RequestParam(required = false) String sortBy,
    // @RequestParam(defaultValue = "asc") String sort) {

    // PaginationCriteria pagination = PaginationCriteria.builder()
    // .page(page)
    // .size(size)
    // .sortBy(sortBy)
    // .sort(sort)
    // .columns(new String[] { "rating", "createdAt", "updatedAt" })
    // .build();

    // Page<ServiceReviewResponse> reviews =
    // serviceReviewService.getProviderReviews(providerId, pagination);
    // return ResponseEntity.ok(reviews);
    // }

    @GetMapping("/provider/{providerId}/rating")
    @Operation(summary = "Get provider average rating", description = "Get the average rating for a service provider")
    public ResponseEntity<Double> getProviderAverageRating(@PathVariable UUID providerId) {
        Double averageRating = serviceReviewService.getProviderAverageRating(providerId);
        return ResponseEntity.ok(averageRating);
    }
}