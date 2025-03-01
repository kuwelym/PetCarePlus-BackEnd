package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.dto.request.ServiceRequest;
import petitus.petcareplus.dto.response.ServiceResponse;
import petitus.petcareplus.exceptions.BadRequestException;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.PetService;
import petitus.petcareplus.repository.ServiceRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceService {
    private final ServiceRepository serviceRepository;

    public List<ServiceResponse> getAllServices() {
        return serviceRepository.findAll().stream()
                .map(this::mapToServiceResponse)
                .collect(Collectors.toList());
    }

    public ServiceResponse getServiceById(UUID id) {
        PetService service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + id));
        return mapToServiceResponse(service);
    }

    @Transactional
    public ServiceResponse createService(ServiceRequest request) {
        // Check if service name already exists
        if (serviceRepository.findByName(request.getName()).isPresent()) {
            throw new BadRequestException("Service name already exists");
        }

        PetService service = PetService.builder()
                .name(request.getName())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .basePrice(request.getBasePrice())
                .build();

        PetService savedService = serviceRepository.save(service);
        return mapToServiceResponse(savedService);
    }

    @Transactional
    public ServiceResponse updateService(UUID id, ServiceRequest request) {
        PetService service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + id));

        // Check if updated name conflicts with another service
        serviceRepository.findByName(request.getName())
                .ifPresent(existingService -> {
                    if (!existingService.getId().equals(id)) {
                        throw new BadRequestException("Service name already exists");
                    }
                });

        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setIconUrl(request.getIconUrl());
        service.setBasePrice(request.getBasePrice());

        PetService updatedService = serviceRepository.save(service);
        return mapToServiceResponse(updatedService);
    }

    @Transactional
    public void deleteService(UUID id) {
        if (!serviceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Service not found with id: " + id);
        }
        serviceRepository.deleteById(id);
    }

    public List<ServiceResponse> searchServices(String query) {
        return serviceRepository.findByNameContainingIgnoreCase(query).stream()
                .map(this::mapToServiceResponse)
                .collect(Collectors.toList());
    }

    private ServiceResponse mapToServiceResponse(PetService service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .iconUrl(service.getIconUrl())
                .basePrice(service.getBasePrice())
                .build();
    }
}