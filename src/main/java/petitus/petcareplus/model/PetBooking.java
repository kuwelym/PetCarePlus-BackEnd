package petitus.petcareplus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "pet_bookings")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetBooking {

    @EmbeddedId
    private PetBookingId id;

    @ManyToOne
    @MapsId("bookingId")
    @JoinColumn(name = "bookings_id")
    private Booking booking;

    @ManyToOne
    @MapsId("petId")
    @JoinColumn(name = "pet_id")
    private Pet pet;

    @ManyToOne
    @MapsId("serviceId")
    @JoinColumn(name = "service_id")
    private DefaultService service;
}