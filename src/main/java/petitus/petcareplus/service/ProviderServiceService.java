package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import petitus.petcareplus.dto.request.service.ProviderServicePatchRequest;
import petitus.petcareplus.dto.request.service.ProviderServiceRequest;
import petitus.petcareplus.dto.response.ProviderServiceResponse;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.ProviderService;
import petitus.petcareplus.model.ProviderServiceId;
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

                petitus.petcareplus.model.PetService service = serviceRepository.findById(request.getServiceId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Service not found with id: " + request.getServiceId()));

                ProviderServiceId id = new ProviderServiceId(providerId, request.getServiceId());

                ProviderService providerService = ProviderService.builder()
                                .id(id)
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
        public ProviderServiceResponse updateProviderService(UUID providerId, UUID serviceId,
                        ProviderServicePatchRequest request) {
                ProviderServiceId id = new ProviderServiceId(providerId, serviceId);

                ProviderService providerService = providerServiceRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Provider service not found"));

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
        public void removeServiceFromProvider(UUID providerId, UUID serviceId) {
                ProviderServiceId id = new ProviderServiceId(providerId, serviceId);

                ProviderService providerService = providerServiceRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Provider service not found"));

                // Soft delete
                providerService.setDeletedAt(LocalDateTime.now());
                providerServiceRepository.save(providerService);
        }

        private ProviderServiceResponse mapToProviderServiceResponse(ProviderService providerService) {
                return ProviderServiceResponse.builder()
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