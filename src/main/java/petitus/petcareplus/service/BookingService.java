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
import petitus.petcareplus.enums.BookingStatus;
import petitus.petcareplus.exceptions.BadRequestException;
import petitus.petcareplus.exceptions.ForbiddenException;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.*;
import petitus.petcareplus.repository.*;
import petitus.petcareplus.utils.Constants;

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
    private final ServiceRepository serviceRepository;
    private final ProviderServiceRepository providerServiceRepository;
    private final MessageSourceService messageSourceService;

    // private static final Logger logger =
    // LoggerFactory.getLogger(BookingService.class);

    @Transactional
    public BookingResponse createBooking(UUID userId, BookingRequest request) {
        // Validate user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("user_not_found")));

        // Validate provider
        User provider = userRepository.findById(request.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("provider_not_found")));

        if (!provider.getRole().getName().equals(Constants.RoleEnum.SERVICE_PROVIDER)) {
            throw new BadRequestException(messageSourceService.get("invalid_provider"));
        }

        // Validate time
        validateBookingTime(request.getScheduledStartTime(), request.getScheduledEndTime());

        // Check provider availability
        checkProviderAvailability(request.getProviderId(), request.getScheduledStartTime(),
                request.getScheduledEndTime());

        // Initialize booking
        Booking booking = Booking.builder()
                .user(user)
                .provider(provider)
                .scheduledStartTime(request.getScheduledStartTime())
                .scheduledEndTime(request.getScheduledEndTime())
                .note(request.getNote())
                .status(BookingStatus.PENDING)
                .paymentStatus("pending")
                .totalPrice(BigDecimal.ZERO)
                .build();

        // Calculate total price and validate services
        Set<ServiceBooking> serviceBookings = new HashSet<>();
        Set<PetBooking> petBookings = new HashSet<>();
        Map<UUID, BigDecimal> servicePrices = new HashMap<>();

        for (PetServiceBookingRequest petServiceReq : request.getPetServices()) {
            // Validate pet ownership
            Pet pet = petRepository.findById(petServiceReq.getPetId())
                    .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("pet_not_found")));

            if (!pet.getUser().getId().equals(userId)) {
                throw new ForbiddenException(messageSourceService.get("pet_not_owned"));
            }

            // Validate service
            PetService service = serviceRepository.findById(petServiceReq.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException(messageSourceService.get("service_not_found")));

            // Verify provider offers this service
            ProviderService providerService = providerServiceRepository.findById(
                    new ProviderServiceId(request.getProviderId(), petServiceReq.getServiceId()))
                    .orElseThrow(
                            () -> new BadRequestException(messageSourceService.get("service_not_offered_by_provider")));

            // Calculate price if not already done
            if (!servicePrices.containsKey(service.getId())) {
                BigDecimal price = providerService.getCustomPrice() != null ? providerService.getCustomPrice()
                        : service.getBasePrice();
                servicePrices.put(service.getId(), price);

                // Add to service bookings
                ServiceBookingId serviceBookingId = new ServiceBookingId(booking.getId(), service.getId());
                ServiceBooking serviceBooking = ServiceBooking.builder()
                        .id(serviceBookingId)
                        .booking(booking)
                        .service(service)
                        .price(price)
                        .build();
                serviceBookings.add(serviceBooking);
            }

            // Add to pet bookings
            PetBookingId petBookingId = new PetBookingId(booking.getId(), pet.getId(), service.getId());
            PetBooking petBooking = PetBooking.builder()
                    .id(petBookingId)
                    .booking(booking)
                    .pet(pet)
                    .service(service)
                    .build();
            petBookings.add(petBooking);
        }

        // Calculate total price
        BigDecimal totalPrice = servicePrices.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        booking.setTotalPrice(totalPrice);

        // Save booking
        Booking savedBooking = bookingRepository.save(booking);

        // Update IDs and save related entities
        serviceBookings.forEach(sb -> {
            ServiceBookingId newId = sb.getId();
            newId.setBookingId(savedBooking.getId());
            sb.setId(newId);
        });
        petBookings.forEach(pb -> {
            PetBookingId newId = pb.getId();
            newId.setBookingId(savedBooking.getId());
            pb.setId(newId);
        });

        serviceBookingRepository.saveAll(serviceBookings);
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
                booking.setCancellationReason(request.getCancellationReason());
                break;
            case COMPLETED:
                booking.setActualEndTime(LocalDateTime.now());
                break;
            case ONGOING:
                // Only provider can mark as ongoing
                if (!isProvider) {
                    throw new ForbiddenException(messageSourceService.get("only_provider_can_mark_ongoing"));
                }
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

    public List<BookingResponse> getUserBookingsByStatus(UUID userId, String statusStr) {
        try {
            BookingStatus status = BookingStatus.fromValue(statusStr);
            return bookingRepository.findAllByUserIdAndStatus(userId, status).stream()
                    .map(this::mapToBookingResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(messageSourceService.get("invalid_booking_status"));
        }
    }

    public List<BookingResponse> getProviderBookingsByStatus(UUID providerId, String statusStr) {
        try {
            BookingStatus status = BookingStatus.fromValue(statusStr);
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

        // TODO: Check provider's available time from ProviderProfile
    }

    private void validateStatusTransition(BookingStatus currentStatus, BookingStatus newStatus, boolean isProvider,
            boolean isUser) {
        // Define valid transitions
        Set<BookingStatus> validTransitions = new HashSet<>();

        switch (currentStatus) {
            case PENDING:
                if (isProvider) {
                    validTransitions.add(BookingStatus.ACCEPTED);
                    validTransitions.add(BookingStatus.CANCELLED);
                }
                if (isUser) {
                    validTransitions.add(BookingStatus.CANCELLED);
                }
                break;
            case ACCEPTED:
                if (isProvider) {
                    validTransitions.add(BookingStatus.ONGOING);
                    validTransitions.add(BookingStatus.CANCELLED);
                }
                if (isUser) {
                    validTransitions.add(BookingStatus.CANCELLED);
                }
                break;
            case ONGOING:
                if (isProvider) {
                    validTransitions.add(BookingStatus.COMPLETED);
                    validTransitions.add(BookingStatus.CANCELLED);
                }
                break;
            case COMPLETED:
            case CANCELLED:
                // No transitions allowed from these terminal states
                validTransitions = Collections.emptySet();
                break;
        }

        if (!validTransitions.contains(newStatus)) {
            throw new BadRequestException(messageSourceService.get("invalid_status_transition"));
        }
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
                .userId(booking.getUser().getId())
                .userName(booking.getUser().getFullName())
                .providerId(booking.getProvider().getId())
                .providerName(booking.getProvider().getFullName())
                .status(booking.getStatus().getValue())
                .totalPrice(booking.getTotalPrice())
                .paymentStatus(booking.getPaymentStatus())
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