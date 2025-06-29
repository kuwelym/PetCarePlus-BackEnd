package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.dto.request.booking.BookingRequest;
import petitus.petcareplus.dto.request.booking.BookingStatusUpdateRequest;
import petitus.petcareplus.dto.request.booking.PetServiceBookingRequest;
import petitus.petcareplus.dto.response.booking.BookingPetServiceResponse;
import petitus.petcareplus.dto.response.booking.BookingResponse;
import petitus.petcareplus.utils.enums.BookingStatus;
import petitus.petcareplus.utils.enums.PaymentStatus;
import petitus.petcareplus.utils.enums.TransactionStatus;
import petitus.petcareplus.utils.enums.TransactionType;
import petitus.petcareplus.exceptions.BadRequestException;
import petitus.petcareplus.exceptions.ForbiddenException;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.*;
import petitus.petcareplus.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PetBookingRepository petBookingRepository;
    private final ServiceBookingRepository serviceBookingRepository;
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    // private final ServiceRepository serviceRepository;
    private final ProviderServiceRepository providerServiceRepository;
    private final MessageSourceService messageSourceService;
    private final WalletService walletService;

    @Transactional
    public BookingResponse createBooking(UUID userId, BookingRequest request) {
        // Validate user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("user_not_found")));

        // Validate provider service
        ProviderService providerService = providerServiceRepository.findById(request.getProviderServiceId())
                .orElseThrow(
                        () -> new ResourceNotFoundException(messageSourceService.get("provider_service_not_found")));

        User provider = providerService.getProvider();
        DefaultService service = providerService.getService();

        // Validate time
        validateBookingTime(request.getScheduledStartTime(), request.getScheduledEndTime());

        // Check provider availability
        checkProviderAvailability(provider.getId(), request.getScheduledStartTime(),
                request.getScheduledEndTime());

        // Initialize booking
        Booking booking = Booking.builder()
                .user(user)
                .provider(provider)
                .scheduledStartTime(request.getScheduledStartTime())
                .scheduledEndTime(request.getScheduledEndTime())
                .note(request.getNote())
                .status(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .totalPrice(BigDecimal.ZERO)
                .build();

        // Calculate total price based on number of pets
        BigDecimal servicePrice = providerService.getCustomPrice() != null ? providerService.getCustomPrice()
                : service.getBasePrice();
        BigDecimal totalPrice = servicePrice.multiply(BigDecimal.valueOf(request.getPetList().size()));
        booking.setTotalPrice(totalPrice);

        // Save booking first
        Booking savedBooking = bookingRepository.save(booking);

        // Create service booking (only one service)
        ServiceBookingId serviceBookingId = new ServiceBookingId(savedBooking.getId(), service.getId());
        ServiceBooking serviceBooking = ServiceBooking.builder()
                .id(serviceBookingId)
                .booking(savedBooking)
                .service(service)
                .price(servicePrice)
                .build();
        serviceBookingRepository.save(serviceBooking);

        // Create pet bookings for multiple pets
        Set<PetBooking> petBookings = new HashSet<>();
        for (PetServiceBookingRequest petSBR : request.getPetList()) {
            // Validate pet ownership
            Pet pet = petRepository.findById(petSBR.getPetId())
                    .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("pet_not_found")));

            if (!pet.getUserId().equals(userId)) {
                throw new ForbiddenException(messageSourceService.get("pet_not_owned"));
            }

            PetBookingId petBookingId = new PetBookingId(savedBooking.getId(), pet.getId(), service.getId());
            PetBooking petBooking = PetBooking.builder()
                    .id(petBookingId)
                    .booking(savedBooking)
                    .pet(pet)
                    .service(service)
                    .build();
            petBookings.add(petBooking);
        }

        petBookingRepository.saveAll(petBookings);

        // Return response
        return mapToBookingResponse(savedBooking);
    }

    @Transactional
    public BookingResponse updateBookingStatus(UUID userId, UUID bookingId, BookingStatusUpdateRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("booking_not_found")));

        // Verify permission to update status
        boolean isProvider = booking.getProvider().getId().equals(userId);
        boolean isUser = booking.getUser().getId().equals(userId);

        if (!isProvider && !isUser) {
            throw new ForbiddenException(messageSourceService.get("booking_update_not_allowed"));
        }

        // Validate status transition
        validateStatusTransition(booking.getStatus(), request.getStatus(), isProvider, isUser);

        // Update status
        booking.setStatus(request.getStatus());

        // Handle specific status updates
        switch (request.getStatus()) {
            case CANCELLED:
                if (request.getCancellationReason() == null) {
                    throw new BadRequestException(messageSourceService.get("cancellation_reason_required"));
                }
                booking.setCancellationReason(request.getCancellationReason());
                break;
            case SERVICE_DONE:
                // Only provider can mark as service done
                // if (!isProvider) {
                // throw new
                // ForbiddenException(messageSourceService.get("only_provider_can_mark_service_done"));
                // }
                booking.setActualEndTime(LocalDateTime.now());
                break;
            case COMPLETED:
                // Only user can mark as completed
                // if (!isUser) {
                // throw new
                // ForbiddenException(messageSourceService.get("only_user_can_mark_completed"));
                // }
                if (booking.getPaymentStatus() != PaymentStatus.COMPLETED) {
                    throw new BadRequestException(messageSourceService.get("payment_required_before_completion"));
                }

                booking.setActualEndTime(LocalDateTime.now());
                walletService.createWalletTransaction(booking.getProvider().getId(), booking.getTotalPrice(),
                        TransactionType.SERVICE_PROVIDER_EARNING, TransactionStatus.COMPLETED, "Payment for provider",
                        bookingId);
                break;
            case ONGOING:
                // Only provider can mark as ongoing
                // if (!isProvider) {
                // throw new
                // ForbiddenException(messageSourceService.get("only_provider_can_mark_ongoing"));
                // }
                break;
            case ACCEPTED:
                // Only provider can accept booking
                // if (!isProvider) {
                // throw new
                // ForbiddenException(messageSourceService.get("only_provider_can_accept_booking"));
                // }
                break;
            default:
                break;
        }

        Booking updatedBooking = bookingRepository.save(booking);
        return mapToBookingResponse(updatedBooking);
    }

    @Transactional
    public void deleteBooking(UUID userId, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("booking_not_found")));

        // Check if user is owner or provider
        if (!booking.getUser().getId().equals(userId) && !booking.getProvider().getId().equals(userId)) {
            throw new ForbiddenException(messageSourceService.get("booking_delete_not_allowed"));
        }

        // Soft delete
        booking.setDeletedAt(LocalDateTime.now());
        bookingRepository.save(booking);
    }

    public BookingResponse getBooking(UUID userId, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("booking_not_found")));

        // Check if user has access to this booking
        if (!booking.getUser().getId().equals(userId) && !booking.getProvider().getId().equals(userId)) {
            throw new ForbiddenException(messageSourceService.get("booking_access_denied"));
        }

        // Check if booking was deleted
        if (booking.getDeletedAt() != null) {
            throw new ResourceNotFoundException(messageSourceService.get("booking_not_found"));
        }

        return mapToBookingResponse(booking);
    }

    public Page<BookingResponse> getUserBookings(UUID userId, Pageable pageable) {
        return bookingRepository.findAllByUserId(userId, pageable)
                .map(this::mapToBookingResponse);
    }

    public Page<BookingResponse> getProviderBookings(UUID providerId, Pageable pageable) {
        return bookingRepository.findAllByProviderId(providerId, pageable)
                .map(this::mapToBookingResponse);
    }

    public List<BookingResponse> getUserBookingsByStatus(UUID userId, BookingStatus status) {
        try {
            return bookingRepository.findAllByUserIdAndStatus(userId, status).stream()
                    .map(this::mapToBookingResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(messageSourceService.get("invalid_booking_status"));
        }
    }

    public List<BookingResponse> getProviderBookingsByStatus(UUID providerId, BookingStatus status) {
        try {
            return bookingRepository.findAllByProviderIdAndStatus(providerId, status).stream()
                    .map(this::mapToBookingResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(messageSourceService.get("invalid_booking_status"));
        }
    }

    public List<BookingResponse> getProviderBookingsForDateRange(UUID providerId, LocalDateTime startDate,
            LocalDateTime endDate) {
        return bookingRepository.findAllByProviderIdBetweenDates(providerId, startDate, endDate).stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    // Helper methods

    private void validateBookingTime(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new BadRequestException(messageSourceService.get("invalid_booking_time_range"));
        }

        if (start.isBefore(LocalDateTime.now())) {
            throw new BadRequestException(messageSourceService.get("booking_time_in_past"));
        }
    }

    private void checkProviderAvailability(UUID providerId, LocalDateTime start, LocalDateTime end) {
        Long overlappingCount = bookingRepository.countOverlappingBookings(providerId, start, end);
        if (overlappingCount > 0) {
            throw new BadRequestException(messageSourceService.get("provider_not_available"));
        }

    }

    // private void validateStatusTransition(BookingStatus currentStatus,
    // BookingStatus newStatus, boolean isProvider,
    // boolean isUser) {
    // // Define valid transitions
    // Set<BookingStatus> validTransitions = new HashSet<>();

    // switch (currentStatus) {
    // case PENDING:
    // if (isProvider) {
    // validTransitions.add(BookingStatus.ACCEPTED);
    // validTransitions.add(BookingStatus.CANCELLED);
    // }
    // if (isUser) {
    // validTransitions.add(BookingStatus.CANCELLED);
    // }
    // break;
    // case ACCEPTED:
    // if (isProvider) {
    // validTransitions.add(BookingStatus.ONGOING);
    // validTransitions.add(BookingStatus.CANCELLED);
    // }
    // if (isUser) {
    // validTransitions.add(BookingStatus.CANCELLED);
    // }
    // break;
    // case ONGOING:
    // if (isProvider) {
    // validTransitions.add(BookingStatus.SERVICE_DONE);
    // validTransitions.add(BookingStatus.CANCELLED);
    // }
    // break;
    // case SERVICE_DONE:
    // if (isUser) {
    // validTransitions.add(BookingStatus.COMPLETED);
    // validTransitions.add(BookingStatus.CANCELLED);
    // }
    // break;
    // case COMPLETED:
    // case CANCELLED:
    // // No transitions allowed from these terminal states
    // validTransitions = Collections.emptySet();
    // break;
    // }

    // if (!validTransitions.contains(newStatus)) {
    // throw new
    // BadRequestException(messageSourceService.get("invalid_status_transition",
    // new Object[] { currentStatus.name(), newStatus.name() }));
    // }
    // }

    private void validateStatusTransition(BookingStatus currentStatus, BookingStatus newStatus, boolean isProvider,
            boolean isUser) {
        // Kiểm tra các trường hợp cụ thể và quăng exception với thông báo phù hợp
        switch (currentStatus) {
            case PENDING:
                if (newStatus == BookingStatus.ACCEPTED) {
                    if (!isProvider) {
                        throw new ForbiddenException(messageSourceService.get("only_provider_can_accept_booking"));
                    }
                    return; // Transition valid
                } else if (newStatus == BookingStatus.CANCELLED) {
                    // Both provider and user can cancel
                    return; // Transition valid
                }
                break;

            case ACCEPTED:
                if (newStatus == BookingStatus.ONGOING) {
                    if (!isProvider) {
                        throw new ForbiddenException(messageSourceService.get("only_provider_can_mark_ongoing"));
                    }
                    return; // Transition valid
                } else if (newStatus == BookingStatus.CANCELLED) {
                    // Both can cancel
                    return; // Transition valid
                }
                break;

            case ONGOING:
                if (newStatus == BookingStatus.SERVICE_DONE) {
                    if (!isProvider) {
                        throw new ForbiddenException(messageSourceService.get("only_provider_can_mark_service_done"));
                    }
                    return; // Transition valid
                } else if (newStatus == BookingStatus.CANCELLED) {
                    if (!isProvider) {
                        throw new ForbiddenException(messageSourceService.get("only_provider_can_cancel_ongoing"));
                    }
                    return; // Transition valid
                }
                break;

            case SERVICE_DONE:
                if (newStatus == BookingStatus.COMPLETED) {
                    if (!isUser) {
                        throw new ForbiddenException(messageSourceService.get("only_user_can_mark_completed"));
                    }
                    return; // Transition valid
                } else if (newStatus == BookingStatus.CANCELLED) {
                    if (!isUser) {
                        throw new ForbiddenException(
                                messageSourceService.get("only_user_can_cancel_after_service_done"));
                    }
                    return; // Transition valid
                }
                break;

            case COMPLETED:
            case CANCELLED:
                // No transitions allowed from these terminal states
                break;
        }

        // Nếu không có trường hợp nào ở trên được xử lý, quăng exception chung
        throw new BadRequestException(messageSourceService.get("invalid_status_transition",
                new Object[] { currentStatus.name(), newStatus.name() }));
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        // Fetch pet bookings
        List<PetBooking> petBookings = petBookingRepository.findByBookingId(booking.getId());
        List<ServiceBooking> serviceBookings = serviceBookingRepository.findByBookingId(booking.getId());

        // Map to response DTOs
        List<BookingPetServiceResponse> petServiceResponses = new ArrayList<>();

        Map<UUID, BigDecimal> servicePriceMap = serviceBookings.stream()
                .collect(Collectors.toMap(sb -> sb.getId().getServiceId(), ServiceBooking::getPrice));

        for (PetBooking pb : petBookings) {
            BookingPetServiceResponse petService = BookingPetServiceResponse.builder()
                    .petId(pb.getId().getPetId())
                    .petName(pb.getPet().getName())
                    .petImageUrl(pb.getPet().getImageUrl())
                    .serviceId(pb.getId().getServiceId())
                    .serviceName(pb.getService().getName())
                    .price(servicePriceMap.get(pb.getId().getServiceId()))
                    .build();
            petServiceResponses.add(petService);
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .serviceName(booking.getProviderService().getService().getName())
                .providerServiceId(booking.getProviderService().getId())
                .userId(booking.getUser().getId())
                .userName(booking.getUser().getFullName())
                .providerId(booking.getProvider().getId())
                .providerName(booking.getProvider().getFullName())
                .status(booking.getStatus().name())
                .totalPrice(booking.getTotalPrice())
                .paymentStatus(booking.getPaymentStatus().name())
                .bookingTime(booking.getBookingTime())
                .scheduledStartTime(booking.getScheduledStartTime())
                .scheduledEndTime(booking.getScheduledEndTime())
                .actualEndTime(booking.getActualEndTime())
                .cancellationReason(booking.getCancellationReason())
                .note(booking.getNote())
                .createdAt(booking.getCreatedAt())
                .petServices(petServiceResponses)
                .build();
    }
}