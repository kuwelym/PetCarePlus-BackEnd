package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.dto.request.booking.BookingRequest;
import petitus.petcareplus.dto.request.booking.BookingStatusUpdateRequest;
import petitus.petcareplus.dto.response.booking.BookingResponse;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.security.jwt.JwtUserDetails;
import petitus.petcareplus.service.BookingService;
import petitus.petcareplus.utils.enums.BookingStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Management", description = "APIs for creating, updating and managing bookings")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasAuthority('USER')")
    @Operation(summary = "Create a new booking", description = "Creates a new booking for a service provider")
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @Valid @RequestBody BookingRequest request) {

        BookingResponse response = bookingService.createBooking(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyAuthority('USER', 'SERVICE_PROVIDER', 'ADMIN')")
    @Operation(summary = "Get booking details", description = "Get details of a specific booking")
    public ResponseEntity<BookingResponse> getBooking(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @PathVariable UUID bookingId) {

        BookingResponse response = bookingService.getBooking(userDetails.getId(), bookingId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{bookingId}/status")
    @PreAuthorize("hasAnyAuthority('USER', 'SERVICE_PROVIDER')")
    @Operation(summary = "Update booking status", description = "Update the status of a booking (e.g., accept, cancel)")
    public ResponseEntity<BookingResponse> updateBookingStatus(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @PathVariable UUID bookingId,
            @Valid @RequestBody BookingStatusUpdateRequest request) {

        BookingResponse response = bookingService.updateBookingStatus(userDetails.getId(), bookingId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{bookingId}")
    @PreAuthorize("hasAnyAuthority('USER', 'SERVICE_PROVIDER', 'ADMIN')")
    @Operation(summary = "Delete a booking", description = "Soft-delete a booking")
    public ResponseEntity<Void> deleteBooking(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @PathVariable UUID bookingId) {

        bookingService.deleteBooking(userDetails.getId(), bookingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user")
    @PreAuthorize("hasAuthority('USER')")
    @Operation(summary = "Get user's bookings", description = "Get all bookings made by the current user")
    public ResponseEntity<Page<BookingResponse>> getUserBookings(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sort) {

        PaginationCriteria pagination = PaginationCriteria.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sort(sort)
                .columns(new String[] { "updateAt", "createdAt" }) // Allowed sort fields
                .build();

        Page<BookingResponse> bookings = bookingService.getUserBookings(userDetails.getId(), pagination);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/provider")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    @Operation(summary = "Get provider's bookings", description = "Get all bookings for the current provider")
    public ResponseEntity<Page<BookingResponse>> getProviderBookings(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sort) {

        PaginationCriteria pagination = PaginationCriteria.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sort(sort)
                .columns(new String[] { "updateAt", "createdAt" }) // Allowed sort fields
                .build();

        Page<BookingResponse> bookings = bookingService.getProviderBookings(userDetails.getId(), pagination);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/user/status/{status}")
    @PreAuthorize("hasAuthority('USER')")
    @Operation(summary = "Get user's bookings by status", description = "Get all bookings with a specific status for the current user")
    public ResponseEntity<Page<BookingResponse>> getUserBookingsByStatus(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @PathVariable BookingStatus status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sort) {

        PaginationCriteria pagination = PaginationCriteria.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sort(sort)
                .columns(new String[] { "updateAt", "createdAt" }) // Allowed sort fields
                .build();

        Page<BookingResponse> bookings = bookingService.getUserBookingsByStatus(userDetails.getId(), status,
                pagination);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/provider/status/{status}")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    @Operation(summary = "Get provider's bookings by status", description = "Get all bookings with a specific status for the current provider")
    public ResponseEntity<Page<BookingResponse>> getProviderBookingsByStatus(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @PathVariable BookingStatus status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sort) {

        PaginationCriteria pagination = PaginationCriteria.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sort(sort)
                .columns(new String[] { "updateAt", "createdAt" }) // Allowed sort fields
                .build();

        Page<BookingResponse> bookings = bookingService.getProviderBookingsByStatus(userDetails.getId(), status,
                pagination);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/provider/schedule")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    @Operation(summary = "Get provider's schedule", description = "Get all bookings for a specific time range")
    public ResponseEntity<Page<BookingResponse>> getProviderSchedule(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @Parameter(description = "Start date-time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End date-time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sort) {

        PaginationCriteria pagination = PaginationCriteria.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sort(sort)
                .columns(new String[] { "scheduledStartTime", "scheduledEndTime" }) // Allowed sort fields
                .build();

        Page<BookingResponse> bookings = bookingService.getProviderBookingsForDateRange(userDetails.getId(), start,
                end, pagination);
        return ResponseEntity.ok(bookings);
    }
}