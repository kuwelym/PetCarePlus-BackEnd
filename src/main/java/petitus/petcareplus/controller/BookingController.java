package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.dto.request.booking.BookingRequest;
import petitus.petcareplus.dto.request.booking.BookingStatusUpdateRequest;
import petitus.petcareplus.dto.response.StandardPaginationResponse;
import petitus.petcareplus.dto.response.booking.BookingResponse;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.security.jwt.JwtUserDetails;
import petitus.petcareplus.service.BookingService;
import petitus.petcareplus.service.MessageSourceService;
import petitus.petcareplus.utils.enums.BookingStatus;

import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Management", description = "APIs for creating, updating and managing bookings")
public class BookingController {

        private final BookingService bookingService;
        private final MessageSourceService messageSourceService;

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
        public ResponseEntity<String> deleteBooking(
                        @AuthenticationPrincipal JwtUserDetails userDetails,
                        @PathVariable UUID bookingId) {

                bookingService.deleteBooking(userDetails.getId(), bookingId);
                return ResponseEntity.ok(messageSourceService.get("booking_deleted_successfully"));
        }

        @GetMapping("/user")
        @PreAuthorize("hasAuthority('USER')")
        @Operation(summary = "Get user's bookings", description = "Get all bookings made by the current user")
        public ResponseEntity<StandardPaginationResponse<BookingResponse>> getUserBookings(
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

                Page<BookingResponse> pageResult = bookingService.getUserBookings(userDetails.getId(), pagination);
                StandardPaginationResponse<BookingResponse> response = new StandardPaginationResponse<>(
                                pageResult,
                                pageResult.getContent());
                return ResponseEntity.ok(response);
        }

        @GetMapping("/provider")
        @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
        @Operation(summary = "Get provider's bookings", description = "Get all bookings for the current provider")
        public ResponseEntity<StandardPaginationResponse<BookingResponse>> getProviderBookings(
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
                StandardPaginationResponse<BookingResponse> response = new StandardPaginationResponse<>(
                                bookings,
                                bookings.getContent());
                return ResponseEntity.ok(response);
        }

        @GetMapping("/user/status/{status}")
        @PreAuthorize("hasAuthority('USER')")
        @Operation(summary = "Get user's bookings by status", description = "Get all bookings with a specific status for the current user")
        public ResponseEntity<StandardPaginationResponse<BookingResponse>> getUserBookingsByStatus(
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

                StandardPaginationResponse<BookingResponse> response = new StandardPaginationResponse<>(
                                bookings,
                                bookings.getContent());
                return ResponseEntity.ok(response);
        }

        @GetMapping("/provider/status/{status}")
        @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
        @Operation(summary = "Get provider's bookings by status", description = "Get all bookings with a specific status for the current provider")
        public ResponseEntity<StandardPaginationResponse<BookingResponse>> getProviderBookingsByStatus(
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
                StandardPaginationResponse<BookingResponse> response = new StandardPaginationResponse<>(
                                bookings,
                                bookings.getContent());
                return ResponseEntity.ok(response);
        }
}