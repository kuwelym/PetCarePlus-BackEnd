package petitus.petcareplus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import petitus.petcareplus.utils.enums.BookingStatus;
import petitus.petcareplus.model.Booking;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID>, JpaSpecificationExecutor<Booking> {

        @Query("SELECT b FROM Booking b WHERE b.deletedAt IS NULL AND b.id = :bookingId")
        Optional<Booking> findById(@Param("bookingId") UUID bookingId);

        @Query("SELECT b FROM Booking b WHERE b.deletedAt IS NULL AND b.user.id = :userId ORDER BY b.createdAt DESC")
        List<Booking> findAllByUserId(@Param("userId") UUID userId);

        @Query("SELECT b FROM Booking b WHERE b.deletedAt IS NULL AND b.provider.id = :providerId ORDER BY b.createdAt DESC")
        List<Booking> findAllByProviderId(@Param("providerId") UUID providerId);

        @Query("SELECT b FROM Booking b WHERE b.deletedAt IS NULL AND b.user.id = :userId ORDER BY b.createdAt DESC")
        Page<Booking> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);

        @Query("SELECT b FROM Booking b WHERE b.deletedAt IS NULL AND b.provider.id = :providerId ORDER BY b.createdAt DESC")
        Page<Booking> findAllByProviderId(@Param("providerId") UUID providerId, Pageable pageable);

        @Query("SELECT b FROM Booking b WHERE b.deletedAt IS NULL AND b.status = :status ORDER BY b.createdAt DESC")
        List<Booking> findAllByStatus(@Param("status") BookingStatus status);

        @Query("SELECT b FROM Booking b WHERE b.deletedAt IS NULL AND b.user.id = :userId AND b.status = :status ORDER BY b.createdAt DESC")
        List<Booking> findAllByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") BookingStatus status);

        @Query("SELECT b FROM Booking b WHERE b.deletedAt IS NULL AND b.user.id = :userId AND b.status = :status ORDER BY b.createdAt DESC")
        Page<Booking> findAllByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") BookingStatus status,
                        Pageable pageable);

        @Query("SELECT b FROM Booking b WHERE b.deletedAt IS NULL AND b.provider.id = :providerId AND b.status = :status ORDER BY b.createdAt DESC")
        List<Booking> findAllByProviderIdAndStatus(@Param("providerId") UUID providerId,
                        @Param("status") BookingStatus status);

        @Query("SELECT b FROM Booking b WHERE b.deletedAt IS NULL AND b.provider.id = :providerId AND b.status = :status ORDER BY b.createdAt DESC")
        Page<Booking> findAllByProviderIdAndStatus(@Param("providerId") UUID providerId,
                        @Param("status") BookingStatus status,
                        Pageable pageable);

        @Query("SELECT b FROM Booking b WHERE b.deletedAt IS NULL AND b.scheduledStartTime >= :startDate AND b.scheduledEndTime <= :endDate ORDER BY b.scheduledStartTime ASC")
        Page<Booking> findAllBetweenDates(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate, Pageable pageable);

        @Query("SELECT b FROM Booking b WHERE b.deletedAt IS NULL AND b.user.id = :userId AND b.scheduledStartTime >= :startDate AND b.scheduledEndTime <= :endDate ORDER BY b.scheduledStartTime ASC")
        Page<Booking> findAllByUserIdBetweenDates(@Param("userId") UUID userId,
                        @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        @Query("SELECT b FROM Booking b WHERE b.deletedAt IS NULL AND b.provider.id = :providerId AND b.scheduledStartTime >= :startDate AND b.scheduledEndTime <= :endDate ORDER BY b.scheduledStartTime ASC")
        Page<Booking> findAllByProviderIdBetweenDates(@Param("providerId") UUID providerId,
                        @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        @Query("SELECT COUNT(b) FROM Booking b WHERE b.deletedAt IS NULL AND b.status <> 'CANCELLED' AND b.provider.id = :providerId AND ((b.scheduledStartTime <= :endTime AND b.scheduledEndTime >= :startTime) OR (b.scheduledStartTime <= :startTime AND b.scheduledEndTime >= :startTime)) AND b.status NOT IN ('CANCELLED', 'COMPLETED')")
        Long countOverlappingBookings(@Param("providerId") UUID providerId, @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        @Query("SELECT COUNT(sb) > 0 FROM ServiceBooking sb WHERE sb.booking.id = :bookingId AND sb.service.id = :serviceId")
        boolean existsByBookingIdAndServiceId(@Param("bookingId") UUID bookingId, @Param("serviceId") UUID serviceId);

        // Check if the user has completed bookings for the provider service
        @Query("SELECT COUNT(b) > 0 FROM Booking b " +
                        "JOIN b.serviceBookings sb " +
                        "JOIN ProviderService ps ON ps.provider.id = b.provider.id AND ps.service.id = sb.service.id " +
                        "WHERE b.user.id = :userId " +
                        "AND ps.id = :providerServiceId " +
                        "AND b.status = 'COMPLETED' " +
                        "AND b.deletedAt IS NULL " +
                        "AND b.actualEndTime > :cutoffDate")
        boolean hasCompletedEligibleBookingForProviderService(
                        @Param("userId") UUID userId,
                        @Param("providerServiceId") UUID providerServiceId,
                        @Param("cutoffDate") LocalDateTime cutoffDate);

        // Lấy top 5 users gần nhất đã booking với provider
        @Query("""
                        SELECT DISTINCT b.user, MAX(b.createdAt) as lastBookingDate, COUNT(b.id) as totalBookings
                        FROM Booking b
                        WHERE b.provider.id = :providerId
                        AND b.deletedAt IS NULL
                        AND b.status != 'CANCELLED'
                        GROUP BY b.user
                        ORDER BY MAX(b.createdAt) DESC
                        """)
        List<Object[]> findTop5RecentUsersByProviderId(@Param("providerId") UUID providerId, Pageable pageable);
}