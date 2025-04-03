package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
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
import petitus.petcareplus.repository.ProviderServiceRepository;
import petitus.petcareplus.repository.ServiceRepository;
import petitus.petcareplus.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProviderServiceService {
        private final ProviderServiceRepository providerServiceRepository;
        private final ServiceRepository serviceRepository;
        private final UserRepository userRepository;

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
                                .findByProviderIdAndServiceId(providerId, serviceId)
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

        public List<ProviderServiceResponse> getProvidersByService(UUID serviceId) {
                return providerServiceRepository.findProvidersByServiceId(serviceId).stream()
                                .map(this::mapToProviderServiceResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public ProviderServiceResponse addServiceToProvider(UUID providerId, ProviderServiceRequest request) {
                User provider = userRepository.findById(providerId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Provider not found with id: " + providerId));

                DefaultService service = serviceRepository.findById(request.getServiceId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Service not found with id: " + request.getServiceId()));

                // Check if provider already offers this service
                if (providerServiceRepository.findByProviderIdAndServiceId(providerId, request.getServiceId())
                                .isPresent()) {
                        throw new BadRequestException("Provider already offers this service");
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
        public ProviderServiceResponse updateProviderService(UUID id, UUID currentUserId,
                        ProviderServicePatchRequest request) {

                ProviderService providerService = providerServiceRepository
                                .findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Provider service not found"));

                // Ensure only the owner can update
                if (!providerService.getProvider().getId().equals(currentUserId)) {
                        throw new ForbiddenException("You are not authorized to update this service");
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
        public void removeServiceFromProvider(UUID id, UUID currentUserId) {

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