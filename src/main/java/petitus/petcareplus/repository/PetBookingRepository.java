package petitus.petcareplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import petitus.petcareplus.model.PetBooking;
import petitus.petcareplus.model.PetBookingId;

import java.util.List;
import java.util.UUID;

@Repository
public interface PetBookingRepository extends JpaRepository<PetBooking, PetBookingId> {

    List<PetBooking> findByBookingId(UUID bookingId);

    List<PetBooking> findByPetId(UUID petId);
}