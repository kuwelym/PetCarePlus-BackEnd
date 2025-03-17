package petitus.petcareplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import petitus.petcareplus.model.ServiceBooking;
import petitus.petcareplus.model.ServiceBookingId;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceBookingRepository extends JpaRepository<ServiceBooking, ServiceBookingId> {

    List<ServiceBooking> findByBookingId(UUID bookingId);

    List<ServiceBooking> findByServiceId(UUID serviceId);
}