package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import petitus.petcareplus.dto.response.service.ServiceResponse;
import petitus.petcareplus.model.spec.criteria.ServiceCriteria;
import petitus.petcareplus.service.ServiceService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "APIs for managing services")
@SecurityRequirement(name = "bearerAuth")
public class ServiceController {
    private final ServiceService serviceService;

    @GetMapping
    @Operation(summary = "Get all default services")
    public ResponseEntity<List<ServiceResponse>> getAllServices() {

        return ResponseEntity.ok(serviceService.getAllServices());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get service by ID")
    public ResponseEntity<ServiceResponse> getServiceById(@PathVariable UUID id) {
        return ResponseEntity.ok(serviceService.getServiceById(id));
    }

    // @PostMapping
    // @PreAuthorize("hasAuthority('ADMIN')")
    // @Operation(summary = "Create a new service - Admin only")
    // public ResponseEntity<ServiceResponse> createService(@Valid @RequestBody
    // ServiceRequest request) {
    // return new ResponseEntity<>(serviceService.createService(request),
    // HttpStatus.CREATED);
    // }

    // @PatchMapping("/{id}")
    // @PreAuthorize("hasAuthority('ADMIN')")
    // @Operation(summary = "Update a service - Admin only")
    // public ResponseEntity<ServiceResponse> updateService(@PathVariable UUID id,
    // @Valid @RequestBody ServicePatchRequest request) {
    // return ResponseEntity.ok(serviceService.updateService(id, request));
    // }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Delete a service - Admin only")
    public ResponseEntity<Void> deleteService(@PathVariable UUID id) {
        serviceService.deleteService(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search/advanced")
    @Operation(summary = "Advanced search default services with filtering")
    public ResponseEntity<List<ServiceResponse>> searchServicesAdvanced(
            // Search & Filter parameters
            @RequestParam(required = false) String query,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        // Tạo ServiceCriteria từ request parameters
        ServiceCriteria criteria = ServiceCriteria.builder()
                .query(query)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .build();

        return ResponseEntity.ok(serviceService.searchServices(criteria));
    }
}