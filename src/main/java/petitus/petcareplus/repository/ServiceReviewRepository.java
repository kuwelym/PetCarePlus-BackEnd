package petitus.petcareplus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import petitus.petcareplus.model.ServiceReview;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceReviewRepository
                extends JpaRepository<ServiceReview, UUID>, JpaSpecificationExecutor<ServiceReview> {
        @Query("SELECT sr FROM ServiceReview sr WHERE sr.deletedAt IS NULL AND sr.id = :id")
        Optional<ServiceReview> findById(@Param("id") UUID id);

        @Query("SELECT sr FROM ServiceReview sr WHERE sr.deletedAt IS NULL AND sr.user.id = :userId ORDER BY sr.createdAt DESC")
        List<ServiceReview> findAllByUserId(@Param("userId") UUID userId);

        @Query("SELECT sr FROM ServiceReview sr WHERE sr.deletedAt IS NULL AND sr.user.id = :userId ORDER BY sr.createdAt DESC")
        Page<ServiceReview> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);

        @Query("SELECT sr FROM ServiceReview sr WHERE sr.deletedAt IS NULL AND sr.providerService.id = :providerServiceId")
        List<ServiceReview> findByProviderServiceId(@Param("providerServiceId") UUID providerServiceId);

        @Query("SELECT sr FROM ServiceReview sr WHERE sr.deletedAt IS NULL AND sr.providerService.id = :providerServiceId")
        Page<ServiceReview> findByProviderServiceId(@Param("providerServiceId") UUID providerServiceId,
                        Pageable pageable);

        @Query("SELECT sr FROM ServiceReview sr WHERE " +
                        "sr.deletedAt IS NULL AND " +
                        "(:userId IS NULL OR sr.user.id = :userId) AND " +
                        "(:providerId IS NULL OR sr.providerService.provider.id = :providerId) AND " +
                        "(:serviceId IS NULL OR sr.providerService.service.id = :serviceId) AND " +
                        "(:providerServiceId IS NULL OR sr.providerService.id = :providerServiceId)")
        Page<ServiceReview> findFilteredReviews(
                        @Param("userId") UUID userId,
                        @Param("providerId") UUID providerId,
                        @Param("serviceId") UUID serviceId,
                        @Param("providerServiceId") UUID providerServiceId,
                        Pageable pageable);

        @Query("SELECT sr FROM ServiceReview sr WHERE sr.deletedAt IS NULL AND sr.providerService.service.id = :serviceId AND sr.providerService.provider.id = :providerId")
        Page<ServiceReview> findAllByServiceIdAndProviderId(
                        @Param("serviceId") UUID serviceId,
                        @Param("providerId") UUID providerId,
                        Pageable pageable);

        @Query("SELECT sr FROM ServiceReview sr WHERE sr.deletedAt IS NULL AND sr.providerService.provider.id = :providerId ORDER BY sr.createdAt DESC")
        List<ServiceReview> findAllByProviderId(@Param("providerId") UUID providerId);

        @Query("SELECT sr FROM ServiceReview sr WHERE sr.deletedAt IS NULL AND sr.providerService.provider.id = :providerId ORDER BY sr.createdAt DESC")
        Page<ServiceReview> findAllByProviderId(@Param("providerId") UUID providerId, Pageable pageable);

        @Query("SELECT sr FROM ServiceReview sr WHERE sr.deletedAt IS NULL AND sr.booking.id = :bookingId AND sr.providerService.service.id = :serviceId AND sr.user.id = :userId")
        Optional<ServiceReview> findByBookingIdAndServiceIdAndUserId(
                        @Param("bookingId") UUID bookingId,
                        @Param("serviceId") UUID serviceId,
                        @Param("userId") UUID userId);

        @Query("SELECT AVG(sr.rating) FROM ServiceReview sr WHERE sr.deletedAt IS NULL AND sr.providerService.provider.id = :providerId")
        Double calculateAverageRatingForProvider(@Param("providerId") UUID providerId);

        // Check if the user has already reviewed the provider service
        @Query("SELECT COUNT(sr) > 0 FROM ServiceReview sr " +
                        "WHERE sr.user.id = :userId " +
                        "AND sr.providerService.id = :providerServiceId " +
                        "AND sr.deletedAt IS NULL")
        boolean hasUserReviewedProviderService(
                        @Param("userId") UUID userId,
                        @Param("providerServiceId") UUID providerServiceId);

        // Count total reviews for a provider
        @Query("SELECT COUNT(sr) FROM ServiceReview sr WHERE sr.deletedAt IS NULL AND sr.providerService.provider.id = :providerId")
        Long countReviewsForProvider(@Param("providerId") UUID providerId);
}