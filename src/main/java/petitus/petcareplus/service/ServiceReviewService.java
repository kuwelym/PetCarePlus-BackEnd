package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.dto.request.review.ServiceReviewRequest;
import petitus.petcareplus.dto.request.review.ServiceReviewUpdateRequest;
import petitus.petcareplus.dto.response.review.ServiceReviewResponse;
import petitus.petcareplus.exceptions.BadRequestException;
import petitus.petcareplus.exceptions.ForbiddenException;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.ProviderService;
import petitus.petcareplus.model.ServiceReview;
import petitus.petcareplus.model.User;
import petitus.petcareplus.model.profile.ServiceProviderProfile;
import petitus.petcareplus.model.spec.ServiceReviewSpecification;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.ServiceReviewCriteria;
import petitus.petcareplus.repository.BookingRepository;
import petitus.petcareplus.repository.ProviderServiceRepository;
import petitus.petcareplus.repository.ServiceReviewRepository;
import petitus.petcareplus.repository.UserRepository;
import petitus.petcareplus.utils.PageRequestBuilder;
import petitus.petcareplus.repository.ServiceProviderProfileRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceReviewService {

    private final ServiceReviewRepository serviceReviewRepository;
    private final BookingRepository bookingRepository;
    private final ProviderServiceRepository providerServiceRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ServiceProviderProfileRepository serviceProviderProfileRepository;
    private final MessageSourceService messageSourceService;

    public Page<ServiceReviewResponse> getAllReviews(ServiceReviewCriteria criteria, PaginationCriteria pagination) {
        // Build specification từ criteria
        Specification<ServiceReview> specification = new ServiceReviewSpecification(criteria);

        // Build page request từ pagination
        PageRequest pageRequest = PageRequestBuilder.build(pagination);

        // Execute query
        Page<ServiceReview> reviews = serviceReviewRepository.findAll(specification, pageRequest);

        // Convert to response DTO
        return reviews.map(this::mapToServiceReviewResponse);
    }

    @Transactional
    public ServiceReviewResponse createReview(ServiceReviewRequest request) {
        UUID userId = userService.getCurrentUserId();
        // Validate booking exists and belongs to user
        ProviderService providerService = providerServiceRepository.findById(request.getProviderServiceId())
                .orElseThrow(
                        () -> new ResourceNotFoundException(messageSourceService.get("provider_service_not_found")));

        // Check if user already reviewed the provider service
        if (serviceReviewRepository.hasUserReviewedProviderService(userId, request.getProviderServiceId())) {
            throw new BadRequestException(messageSourceService.get("review_already_exists"));
        }

        // Calculate cutoff date for booking (30 days)
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

        // Validate booking exists and belongs to user
        boolean hasEligibleBooking = bookingRepository.hasCompletedEligibleBookingForProviderService(
                userId, request.getProviderServiceId(), cutoffDate);

        if (!hasEligibleBooking) {
            throw new BadRequestException(
                    messageSourceService.get("no_eligible_booking_found"));
        }

        // Get user, provider and service
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("user_not_found")));

        // Create review
        ServiceReview review = ServiceReview.builder()
                .user(user)
                .providerService(providerService)
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ServiceReview savedReview = serviceReviewRepository.save(review);

        // Update provider's average rating
        updateProviderRating(providerService.getProvider().getId());

        return mapToServiceReviewResponse(savedReview);
    }

    @Transactional
    public ServiceReviewResponse updateReview(UUID reviewId, ServiceReviewUpdateRequest request) {
        UUID userId = userService.getCurrentUserId();
        ServiceReview review = serviceReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("review_not_found")));

        // Validate user owns the review
        if (!review.getUser().getId().equals(userId)) {
            throw new ForbiddenException(messageSourceService.get("not_your_review"));
        }

        // Save previous values
        review.setRatingHistory(review.getRating());
        review.setCommentHistory(review.getComment());

        // Update review
        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }

        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }

        review.setUpdatedAt(LocalDateTime.now());

        ServiceReview updatedReview = serviceReviewRepository.save(review);

        // Update provider's average rating
        updateProviderRating(review.getProviderService().getProvider().getId());

        return mapToServiceReviewResponse(updatedReview);
    }

    @Transactional
    public void deleteReview(UUID reviewId) {
        UUID userId = userService.getCurrentUserId();
        ServiceReview review = serviceReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("review_not_found")));

        // Validate user owns the review or is admin
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("user_not_found")));

        boolean isAdmin = user.getRole().getName().toString().equals("ADMIN");

        if (!review.getUser().getId().equals(userId) && !isAdmin) {
            throw new ForbiddenException(messageSourceService.get("not_your_review"));
        }

        // Soft delete review
        review.setDeletedAt(LocalDateTime.now());
        serviceReviewRepository.save(review);

        // Update provider's average rating
        updateProviderRating(review.getProviderService().getProvider().getId());
    }

    public ServiceReviewResponse getReview(UUID reviewId) {
        ServiceReview review = serviceReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("review_not_found")));

        if (review.getDeletedAt() != null) {
            throw new ResourceNotFoundException(messageSourceService.get("review_not_found"));
        }

        return mapToServiceReviewResponse(review);
    }

    public Page<ServiceReviewResponse> getUserReviews(PaginationCriteria pagination) {
        UUID userId = userService.getCurrentUserId();
        ServiceReviewCriteria criteria = ServiceReviewCriteria.builder()
                .userId(userId)
                .build();
        return getAllReviews(criteria, pagination);
    }

    public Page<ServiceReviewResponse> getServiceReviews(UUID providerServiceId, ServiceReviewCriteria criteria,
            PaginationCriteria pagination) {
        // Validate provider service exists
        if (!providerServiceRepository.existsById(providerServiceId)) {
            throw new ResourceNotFoundException(messageSourceService.get("provider_service_not_found"));
        }
        criteria.setProviderServiceId(providerServiceId);
        return getAllReviews(criteria, pagination);
    }

    // public Page<ServiceReviewResponse> getServiceReviewsByProviderService(UUID
    // serviceId, UUID providerId,
    // Pageable pageable) {
    // return serviceReviewRepository.findAllByServiceIdAndProviderId(serviceId,
    // providerId, pageable)
    // .map(this::mapToServiceReviewResponse);
    // }

    public Page<ServiceReviewResponse> getProviderReviews(UUID providerId, PaginationCriteria pagination) {
        // Validate provider exists
        if (!userRepository.existsById(providerId)) {
            throw new ResourceNotFoundException(messageSourceService.get("provider_not_found"));
        }
        ServiceReviewCriteria criteria = ServiceReviewCriteria.builder()
                .providerId(providerId)
                .build();
        return getAllReviews(criteria, pagination);
    }

    public Double getProviderAverageRating(UUID providerId) {

        // Check if provider exists
        if (!userRepository.existsById(providerId)) {
            throw new ResourceNotFoundException(messageSourceService.get("provider_not_found"));
        }

        Double averageRating = serviceReviewRepository.calculateAverageRatingForProvider(providerId);
        return averageRating != null ? averageRating : 0.0;
    }

    public Long getProviderReviewCount(UUID providerId) {
        // Check if provider exists
        if (!userRepository.existsById(providerId)) {
            throw new ResourceNotFoundException(messageSourceService.get("provider_not_found"));
        }

        Long reviewCount = serviceReviewRepository.countReviewsForProvider(providerId);
        return reviewCount != null ? reviewCount : 0L;
    }

    @Transactional
    public void updateProviderRating(UUID providerId) {
        Double averageRating = getProviderAverageRating(providerId);

        // Find provider profile
        List<ServiceProviderProfile> profiles = serviceProviderProfileRepository
                .findAll()
                .stream()
                .filter(profile -> profile.getProfile().getUser().getId().equals(providerId))
                .collect(Collectors.toList());

        if (!profiles.isEmpty()) {
            ServiceProviderProfile profile = profiles.get(0);
            profile.setRating(averageRating);
            serviceProviderProfileRepository.save(profile);
        }
    }

    private ServiceReviewResponse mapToServiceReviewResponse(ServiceReview review) {
        return ServiceReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFullName())
                .userAvatar(review.getUser().getProfile() != null ? review.getUser().getProfile().getAvatarUrl() : null)
                .providerId(review.getProviderService().getProvider().getId())
                .providerName(review.getProviderService().getProvider().getFullName())
                .serviceId(review.getProviderService().getService().getId())
                .serviceName(review.getProviderService().getService().getName())
                .providerServiceId(review.getProviderService().getId())
                .bookingId(review.getBooking() != null ? review.getBooking().getId() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .ratingHistory(review.getRatingHistory())
                .commentHistory(review.getCommentHistory())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}