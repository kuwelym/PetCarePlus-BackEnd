package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import petitus.petcareplus.dto.request.service.ServicePatchRequest;
import petitus.petcareplus.dto.request.service.ServiceRequest;
import petitus.petcareplus.dto.response.service.AdminServiceResponse;
import petitus.petcareplus.dto.response.service.ServiceResponse;
import petitus.petcareplus.dto.response.service.ServiceResponseForProvider;
import petitus.petcareplus.exceptions.BadRequestException;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.DefaultService;
import petitus.petcareplus.model.ProviderService;
import petitus.petcareplus.model.spec.ServiceFilterSpecification;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.ServiceCriteria;
import petitus.petcareplus.repository.ServiceRepository;
import petitus.petcareplus.utils.PageRequestBuilder;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceService {
    private final ServiceRepository serviceRepository;
    private final ProviderServiceService providerServiceService;

    // old method
    public List<ServiceResponse> getAllServices() {
        return serviceRepository.findAll().stream()
                .map(this::mapToServiceResponse)
                .collect(Collectors.toList());
    }

    public List<ServiceResponseForProvider> getAllServicesForCurrentProvider(UUID providerId) {
        List<DefaultService> services = serviceRepository.findAll();

        if (services.isEmpty()) {
            return List.of(); // Return empty list if no services found
        }

        // Lấy danh sách services mà provider đã có
        List<ProviderService> providerServices = providerServiceService.getProviderServicesByProviderId(providerId);
        Set<UUID> existingServiceIds = providerServices.stream()
                .map(ps -> ps.getService().getId())
                .collect(Collectors.toSet());

        return services.stream()
                .map(service -> {
                    boolean isAvailable = !existingServiceIds.contains(service.getId());
                    return mapToServiceResponseForProvider(service, isAvailable);
                })
                .collect(Collectors.toList());
    }

    // new method with pagination
    public List<ServiceResponse> getAllServices(PaginationCriteria pagination) {
        // PageRequest pageRequest = PageRequestBuilder.build(pagination);
        List<DefaultService> services = serviceRepository.findAll();

        return services.stream()
                .map(this::mapToServiceResponse)
                .collect(Collectors.toList());
    }

    public ServiceResponse getServiceById(UUID id) {
        DefaultService service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + id));
        // Check if the current user is an admin to return full details

        return mapToServiceResponse(service);
    }

    @Transactional
    public AdminServiceResponse createService(ServiceRequest request) {
        // Check if service name already exists
        if (serviceRepository.findByName(request.getName()).isPresent()) {
            throw new BadRequestException("Service name already exists");
        }

        DefaultService service = DefaultService.builder()
                .name(request.getName())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .basePrice(request.getBasePrice())
                .build();

        DefaultService savedService = serviceRepository.save(service);

        return mapToAdminServiceResponse(savedService);
    }

    @Transactional
    public AdminServiceResponse updateService(UUID id, ServicePatchRequest request) {
        DefaultService service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + id));

        // Check if updated name conflicts with another service
        if (request.getName() != null) {
            serviceRepository.findByName(request.getName())
                    .ifPresent(existingService -> {
                        if (!existingService.getId().equals(id)) {
                            throw new BadRequestException("Service name already exists");
                        }
                    });
            service.setName(request.getName());
        }

        if (request.getDescription() != null) {
            service.setDescription(request.getDescription());
        }

        if (request.getIconUrl() != null) {
            service.setIconUrl(request.getIconUrl());
        }

        if (request.getBasePrice() != null) {
            service.setBasePrice(request.getBasePrice());
        }

        DefaultService updatedService = serviceRepository.save(service);

        return mapToAdminServiceResponse(updatedService);
    }

    @Transactional
    public void deleteService(UUID id) {
        if (!serviceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Service not found with id: " + id);
        }
        serviceRepository.deleteById(id);
    }

    public List<ServiceResponse> searchServices(ServiceCriteria criteria) {
        // Tạo Specification từ criteria
        Specification<DefaultService> specification = new ServiceFilterSpecification(criteria);

        // Tạo PageRequest từ pagination
        // PageRequest pageRequest = PageRequestBuilder.build(pagination);

        // Execute query với pagination + filtering
        // Page<DefaultService> services = serviceRepository.findAll(specification,
        // pageRequest);

        List<DefaultService> services = serviceRepository.findAll(specification);

        // Convert Entity → Response DTO
        return services.stream()
                .map(this::mapToServiceResponse)
                .collect(Collectors.toList());
    }

    public Page<AdminServiceResponse> getAllServicesForAdmin(PaginationCriteria pagination) {
        PageRequest pageRequest = PageRequestBuilder.build(pagination);
        Page<DefaultService> services = serviceRepository.findAll(pageRequest);
        return services.map(this::mapToAdminServiceResponse);
    }

    public AdminServiceResponse getServiceByIdForAdmin(UUID id) {
        DefaultService service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + id));
        return mapToAdminServiceResponse(service);
    }

    public Page<AdminServiceResponse> searchServicesForAdmin(ServiceCriteria criteria, PaginationCriteria pagination) {
        Specification<DefaultService> specification = new ServiceFilterSpecification(criteria);
        PageRequest pageRequest = PageRequestBuilder.build(pagination);
        Page<DefaultService> services = serviceRepository.findAll(specification, pageRequest);
        return services.map(this::mapToAdminServiceResponse);
    }

    private ServiceResponse mapToServiceResponse(DefaultService service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .iconUrl(service.getIconUrl())
                .basePrice(service.getBasePrice())
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .build();
    }

    private AdminServiceResponse mapToAdminServiceResponse(DefaultService service) {
        return AdminServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .iconUrl(service.getIconUrl())
                .basePrice(service.getBasePrice())
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .deletedAt(service.getDeletedAt())
                .build();
    }

    private ServiceResponseForProvider mapToServiceResponseForProvider(DefaultService service,
            boolean isServiceAvailable) {
        return ServiceResponseForProvider.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .iconUrl(service.getIconUrl())
                .basePrice(service.getBasePrice())
                .serviceAvailable(isServiceAvailable)
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .build();
    }
}