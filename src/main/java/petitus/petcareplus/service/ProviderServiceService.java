package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import petitus.petcareplus.dto.request.service.ProviderServicePatchRequest;
import petitus.petcareplus.dto.request.service.ProviderServiceRequest;
import petitus.petcareplus.dto.response.service.ProviderServiceResponse;
import petitus.petcareplus.exceptions.BadRequestException;
import petitus.petcareplus.exceptions.ForbiddenException;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.DefaultService;
import petitus.petcareplus.model.ProviderService;
import petitus.petcareplus.model.User;
import petitus.petcareplus.model.spec.ProviderServiceSpecification;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.ProviderServiceCriteria;
import petitus.petcareplus.repository.ProviderServiceRepository;
import petitus.petcareplus.repository.ServiceRepository;
import petitus.petcareplus.utils.PageRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderServiceService {
        private final ProviderServiceRepository providerServiceRepository;
        private final ServiceRepository serviceRepository;
        private final UserService userService;
        private final MessageSourceService messageSourceService;

        private static final BigDecimal MAX_CUSTOM_PRICE = new BigDecimal("10000000"); // 10 million

        // New method
        public Page<ProviderServiceResponse> getAllProviderServices(ProviderServiceCriteria criteria,
                        PaginationCriteria pagination) {
                // Build specification từ criteria
                Specification<ProviderService> specification = buildSpecification(criteria);

                // Build page request từ pagination
                PageRequest pageRequest = PageRequestBuilder.build(pagination);

                // Execute query
                Page<ProviderService> providerServices = providerServiceRepository.findAll(specification, pageRequest);

                // Convert to response DTO
                return providerServices.map(this::mapToProviderServiceResponse);
        }

        // Old method
        public List<ProviderServiceResponse> getAllProviderServices() {
                return providerServiceRepository.findAll().stream()
                                .filter(ps -> ps.getDeletedAt() == null)
                                .map(this::mapToProviderServiceResponse)
                                .collect(Collectors.toList());
        }

        public ProviderServiceResponse getProviderServiceById(UUID id) {
                ProviderService providerService = providerServiceRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Provider service not found with id: " + id));
                return mapToProviderServiceResponse(providerService);
        }

        public ProviderServiceResponse getProviderServiceByProviderAndService(UUID providerId, UUID serviceId) {
                ProviderService providerService = providerServiceRepository
                                .findActiveByProviderIdAndServiceId(providerId, serviceId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Provider service not found with provider id: " + providerId
                                                                + " and service id: " + serviceId));
                return mapToProviderServiceResponse(providerService);
        }

        public List<ProviderServiceResponse> getProviderServices(UUID providerId) {
                return providerServiceRepository.findActiveServicesByProviderId(providerId).stream()
                                .map(this::mapToProviderServiceResponse)
                                .collect(Collectors.toList());
        }

        public List<ProviderService> getProviderServicesByProviderId(UUID providerId) {
                return providerServiceRepository.findActiveServicesByProviderId(providerId);
        }

        @Transactional
        public ProviderServiceResponse addServiceToProvider(ProviderServiceRequest request) {
                User provider = userService.getUser();

                DefaultService service = serviceRepository.findById(request.getServiceId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Service not found with id: " + request.getServiceId()));

                Optional<ProviderService> existingService = providerServiceRepository
                                .findByProviderIdAndServiceId(provider.getId(), request.getServiceId());

                // Check if provider already offers this service
                if (existingService
                                .isPresent()) {
                        ProviderService existing = existingService.get();
                        if (existing.getDeletedAt() != null) {
                                // Nếu đã soft deleted, phục hồi lại
                                existing.setDeletedAt(null);
                                existing.setCustomPrice(request.getCustomPrice() != null ? request.getCustomPrice()
                                                : service.getBasePrice());
                                existing.setCustomDescription(
                                                request.getCustomDescription() != null ? request.getCustomDescription()
                                                                : service.getDescription());

                                ProviderService restoredProviderService = providerServiceRepository.save(existing);
                                return mapToProviderServiceResponse(restoredProviderService);
                        } else {
                                throw new BadRequestException("Provider already offers this service");
                        }

                }

                ProviderService providerService = ProviderService.builder()
                                .provider(provider)
                                .service(service)
                                .customPrice(request.getCustomPrice() != null ? request.getCustomPrice()
                                                : service.getBasePrice())
                                .customDescription(
                                                request.getCustomDescription() != null ? request.getCustomDescription()
                                                                : service.getDescription())
                                .createdAt(LocalDateTime.now())
                                .build();

                ProviderService savedProviderService = providerServiceRepository.save(providerService);
                return mapToProviderServiceResponse(savedProviderService);
        }

        @Transactional
        public ProviderServiceResponse updateProviderService(UUID id,
                        ProviderServicePatchRequest request) {
                UUID currentUserId = userService.getCurrentUserId();

                ProviderService providerService = providerServiceRepository
                                .findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Provider service not found"));

                // Ensure only the owner can update
                if (!providerService.getProvider().getId().equals(currentUserId)) {
                        throw new ForbiddenException("You are not authorized to update this service");
                }

                // Limit custom price more than 10 million
                if (request.getCustomPrice() != null
                                && request.getCustomPrice().compareTo(MAX_CUSTOM_PRICE) > 0) {
                        throw new BadRequestException(messageSourceService.get("custom_price_exceeds_limit"));
                }

                if (request.getCustomPrice() != null) {
                        providerService.setCustomPrice(request.getCustomPrice());
                }

                if (request.getCustomDescription() != null) {
                        providerService.setCustomDescription(request.getCustomDescription());
                }

                ProviderService updatedProviderService = providerServiceRepository.save(providerService);
                return mapToProviderServiceResponse(updatedProviderService);
        }

        @Transactional
        public void removeServiceFromProvider(UUID id) {
                UUID currentUserId = userService.getCurrentUserId();

                ProviderService providerService = providerServiceRepository
                                .findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Provider service not found"));

                // Ensure only the owner can delete
                if (!providerService.getProvider().getId().equals(currentUserId)) {
                        throw new ForbiddenException("You are not authorized to delete this service");
                }
                // Soft delete
                providerService.setDeletedAt(LocalDateTime.now());
                providerServiceRepository.save(providerService);
        }

        private Specification<ProviderService> buildSpecification(ProviderServiceCriteria criteria) {
                if (criteria == null) {
                        log.info("ProviderServiceCriteria is null, returning all active services");
                        // Default: chỉ lấy active services
                        return ProviderServiceSpecification.isActive();
                }

                return Specification
                                .where(ProviderServiceSpecification.searchByQuery(criteria.getQuery()))
                                .and(ProviderServiceSpecification.byProviderId(criteria.getProviderId()))
                                .and(ProviderServiceSpecification.byServiceId(criteria.getServiceId()))
                                .and(ProviderServiceSpecification.priceGreaterThanOrEqual(criteria.getMinCustomPrice()))
                                .and(ProviderServiceSpecification.priceLessThanOrEqual(criteria.getMaxCustomPrice()))
                                // .and(ProviderServiceSpecification.byLocation(criteria.getLocation()))
                                .and(ProviderServiceSpecification.createdAfter(criteria.getCreatedAtStart()))
                                .and(ProviderServiceSpecification.createdBefore(criteria.getCreatedAtEnd()))
                                .and(ProviderServiceSpecification.isDeleted(criteria.getIsDeleted()));
        }

        private ProviderServiceResponse mapToProviderServiceResponse(ProviderService providerService) {
                return ProviderServiceResponse.builder()
                                .id(providerService.getId())
                                .providerId(providerService.getProvider().getId())
                                .serviceId(providerService.getService().getId())
                                .serviceName(providerService.getService().getName())
                                .providerName(providerService.getProvider().getFullName())
                                .basePrice(providerService.getService().getBasePrice())
                                .customPrice(providerService.getCustomPrice())
                                .customDescription(providerService.getCustomDescription())
                                .iconUrl(providerService.getService().getIconUrl())
                                .createdAt(providerService.getCreatedAt())
                                .build();
        }
}