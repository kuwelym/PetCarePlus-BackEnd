package petitus.petcareplus.controller.admin;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import petitus.petcareplus.dto.response.StandardPaginationResponse;
import petitus.petcareplus.dto.response.booking.AdminBookingResponse;
import petitus.petcareplus.model.spec.criteria.BookingCriteria;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.service.BookingService;
import petitus.petcareplus.utils.enums.BookingStatus;
import petitus.petcareplus.utils.enums.PaymentStatus;

@RestController
@RequestMapping("/admin/bookings")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminBookingController {

    private final BookingService bookingService;

    @GetMapping
    @Operation(summary = "Get all bookings with pagination")
    public ResponseEntity<StandardPaginationResponse<AdminBookingResponse>> getAllBookings(

            // Search & Filter parameters
            @RequestParam(required = false) String query,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID providerId,
            @RequestParam(required = false) Boolean isDeleted,

            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sort) {

        BookingCriteria criteria = BookingCriteria.builder()
                .query(query) // Search by comment or user name
                .status(status) // Filter by booking status
                .paymentStatus(paymentStatus) // Filter by payment status
                .userId(userId) // Filter by user ID
                .providerId(providerId) // Filter by provider ID
                .isDeleted(isDeleted) // Filter by deleted status (for admin)
                .build();

        PaginationCriteria pagination = PaginationCriteria.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sort(sort)
                .columns(new String[] { "createdAt", "totalPrice", "bookingTime" }) // Allowed
                                                                                    // sort
                                                                                    // fields
                .build();

        Page<AdminBookingResponse> pageResult = bookingService.getAllBookingsForAdmin(pagination, criteria);
        StandardPaginationResponse<AdminBookingResponse> response = new StandardPaginationResponse<>(
                pageResult,
                pageResult.getContent());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID")
    public ResponseEntity<AdminBookingResponse> getBookingById(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.getBookingByIdForAdmin(id));
    }

}
