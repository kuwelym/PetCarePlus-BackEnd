package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import petitus.petcareplus.dto.response.service.ServiceResponse;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.ServiceCriteria;
import petitus.petcareplus.service.ServiceService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "APIs for managing services")
@SecurityRequirement(name = "bearerAuth")
public class ServiceController {
    private final ServiceService serviceService;

    @GetMapping
    @Operation(summary = "Get all services with pagination")
    public ResponseEntity<Page<ServiceResponse>> getAllServices(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sort) {
        PaginationCriteria pagination = PaginationCriteria.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sort(sort)
                .columns(new String[] { "name", "basePrice", "createdAt" }) // Allowed sort fields
                .build();

        return ResponseEntity.ok(serviceService.getAllServices(pagination));
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
    @Operation(summary = "Advanced search services with pagination and filtering")
    public ResponseEntity<Page<ServiceResponse>> searchServicesAdvanced(
            // Search & Filter parameters
            @RequestParam(required = false) String query,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAtStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAtEnd,

            // Pagination parameters
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sort) {
        // Tạo ServiceCriteria từ request parameters
        ServiceCriteria criteria = ServiceCriteria.builder()
                .query(query)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .build();

        // Tạo PaginationCriteria từ request parameters
        PaginationCriteria pagination = PaginationCriteria.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sort(sort)
                .columns(new String[] { "name", "basePrice", "createdAt" }) // Allowed sort columns
                .build();

        return ResponseEntity.ok(serviceService.searchServices(criteria, pagination));
    }
}