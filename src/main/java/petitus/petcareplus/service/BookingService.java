package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.dto.request.booking.BookingRequest;
import petitus.petcareplus.dto.request.booking.BookingStatusUpdateRequest;
import petitus.petcareplus.dto.request.booking.PetServiceBookingRequest;
import petitus.petcareplus.dto.response.booking.AdminBookingResponse;
import petitus.petcareplus.dto.response.booking.BookingPetServiceResponse;
import petitus.petcareplus.dto.response.booking.BookingResponse;
import petitus.petcareplus.dto.response.service.ProviderServiceResponse;
import petitus.petcareplus.dto.response.user.UserResponse;
import petitus.petcareplus.utils.PageRequestBuilder;
import petitus.petcareplus.utils.enums.BookingStatus;
import petitus.petcareplus.utils.enums.PaymentStatus;
import petitus.petcareplus.utils.enums.TransactionStatus;
import petitus.petcareplus.utils.enums.TransactionType;
import petitus.petcareplus.exceptions.BadRequestException;
import petitus.petcareplus.exceptions.ForbiddenException;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.*;
import petitus.petcareplus.model.spec.BookingFilterSpecification;
import petitus.petcareplus.model.spec.criteria.BookingCriteria;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.wallet.Wallet;
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

    // Limit
    private static final BigDecimal MAX_TOTAL_PRICE = new BigDecimal("500000000");

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
                .providerService(providerService)
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

        if (totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(messageSourceService.get("booking_total_price_invalid"));
        }

        if (totalPrice.compareTo(MAX_TOTAL_PRICE) > 0) {
            throw new BadRequestException(messageSourceService.get("booking_total_price_exceeds_limit"));
        }

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
                // walletService.createWalletTransaction(booking.getProvider().getId(),
                // booking.getTotalPrice(),
                // TransactionType.SERVICE_PROVIDER_EARNING, TransactionStatus.COMPLETED,
                // "Payment for provider",
                // bookingId);
                handleWalletAfterPaymentSuccess(booking);
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

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public Page<BookingResponse> getUserBookings(UUID userId, PaginationCriteria pagination) {
        PageRequest pageRequest = PageRequestBuilder.build(pagination);
        Page<Booking> bookings = bookingRepository.findAllByUserId(userId, pageRequest);
        return bookings.map(this::mapToBookingResponse);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getProviderBookings(UUID providerId, PaginationCriteria pagination) {
        PageRequest pageRequest = PageRequestBuilder.build(pagination);
        Page<Booking> bookings = bookingRepository.findAllByProviderId(providerId, pageRequest);
        return bookings.map(this::mapToBookingResponse);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getUserBookingsByStatus(UUID userId, BookingStatus status,
            PaginationCriteria pagination) {
        try {

            PageRequest pageRequest = PageRequestBuilder.build(pagination);
            Page<Booking> bookings = bookingRepository.findAllByUserIdAndStatus(userId, status, pageRequest);
            return bookings.map(this::mapToBookingResponse);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(messageSourceService.get("invalid_booking_status"));
        }
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getProviderBookingsByStatus(UUID providerId, BookingStatus status,
            PaginationCriteria pagination) {
        try {
            PageRequest pageRequest = PageRequestBuilder.build(pagination);
            Page<Booking> bookings = bookingRepository.findAllByProviderIdAndStatus(providerId, status, pageRequest);
            return bookings.map(this::mapToBookingResponse);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(messageSourceService.get("invalid_booking_status"));
        }
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getProviderBookingsForDateRange(UUID providerId, LocalDateTime startDate,
            LocalDateTime endDate, PaginationCriteria pagination) {
        PageRequest pageRequest = PageRequestBuilder.build(pagination);
        Page<Booking> bookings = bookingRepository.findAllByProviderIdBetweenDates(providerId, startDate, endDate,
                pageRequest);
        return bookings.map(this::mapToBookingResponse);
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

    @Transactional
    public void handleWalletAfterPaymentSuccess(Booking booking) {

        // Add money to provider wallet
        UUID providerId = booking.getProvider().getId();

        Payment payment = booking.getPayment();

        if (payment == null || payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BadRequestException(messageSourceService.get("payment_required_before_wallet_update"));
        }
        BigDecimal amount = payment.getAmount();
        // Calculate platform fee (e.g., 5%)
        BigDecimal platformFee = amount.multiply(new BigDecimal("0.05"));
        BigDecimal providerEarning = amount.subtract(platformFee);

        // Add to provider wallet
        walletService.createWalletTransaction(
                providerId,
                providerEarning,
                TransactionType.SERVICE_PROVIDER_EARNING,
                TransactionStatus.COMPLETED,
                "Earnings from booking: " + booking.getId(),
                booking.getId());

        // Update wallet balance
        Wallet providerWallet = walletService.getWalletByUserId(providerId);
        providerWallet.setBalance(providerWallet.getBalance().add(providerEarning));
        walletService.updateWallet(providerWallet);
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

    private AdminBookingResponse mapToAdminBookingResponse(Booking booking) {
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

        UserResponse userResponse = UserResponse.builder()
                .id(booking.getUser().getId().toString())
                .name(booking.getUser().getName())
                .lastName(booking.getUser().getLastName())
                .role(booking.getUser().getRole().getName().getValue())
                .email(booking.getUser().getEmail())
                .createdAt(booking.getUser().getCreatedAt())
                .updatedAt(booking.getUser().getUpdatedAt())
                .build();

        ProviderServiceResponse providerServiceResponse = ProviderServiceResponse.builder()
                .id(booking.getProviderService().getId())
                .providerId(booking.getProviderService().getProvider().getId())
                .serviceId(booking.getProviderService().getService().getId())
                .serviceName(booking.getProviderService().getService().getName())
                .iconUrl(booking.getProviderService().getService().getIconUrl())
                .customPrice(booking.getProviderService().getCustomPrice())
                .basePrice(booking.getProviderService().getService().getBasePrice())
                .customDescription(booking.getProviderService().getCustomDescription())
                .providerId(booking.getProvider().getId())
                .providerName(booking.getProvider().getFullName())
                .build();

        return AdminBookingResponse.builder()
                .id(booking.getId())
                .user(userResponse)
                .providerService(providerServiceResponse)
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
                .petList(petServiceResponses)
                .updatedAt(booking.getUpdatedAt())
                .deletedAt(booking.getDeletedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<AdminBookingResponse> getAllBookingsForAdmin(PaginationCriteria pagination,
            BookingCriteria criteria) {

        Specification<Booking> specification = new BookingFilterSpecification(criteria);
        PageRequest pageRequest = PageRequestBuilder.build(pagination);
        Page<Booking> bookings = bookingRepository.findAll(specification, pageRequest);

        return bookings.map(this::mapToAdminBookingResponse);
    }

    @Transactional(readOnly = true)
    public AdminBookingResponse getBookingByIdForAdmin(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("booking_not_found")));

        // Check if booking was deleted
        if (booking.getDeletedAt() != null) {
            throw new ResourceNotFoundException(messageSourceService.get("booking_not_found"));
        }

        return mapToAdminBookingResponse(booking);
    }
}