package petitus.petcareplus.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import petitus.petcareplus.dto.request.service.ServicePatchRequest;
import petitus.petcareplus.dto.request.service.ServiceRequest;
import petitus.petcareplus.dto.response.service.AdminServiceResponse;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.ServiceCriteria;
import petitus.petcareplus.service.ServiceService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/admin/services")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminDefaultServiceController {
    private final ServiceService serviceService;

    @GetMapping
    @Operation(summary = "Get all services with pagination")
    public ResponseEntity<Page<AdminServiceResponse>> getAllServices(
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

        return ResponseEntity.ok(serviceService.getAllServicesForAdmin(pagination));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get service by ID")
    public ResponseEntity<AdminServiceResponse> getServiceById(@PathVariable UUID id) {
        return ResponseEntity.ok(serviceService.getServiceByIdForAdmin(id));
    }

    @PostMapping
    @Operation(summary = "Create a new service - Admin only")
    public ResponseEntity<AdminServiceResponse> createService(@Valid @RequestBody ServiceRequest request) {
        return new ResponseEntity<>(serviceService.createService(request), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a service - Admin only")
    public ResponseEntity<AdminServiceResponse> updateService(@PathVariable UUID id,
            @Valid @RequestBody ServicePatchRequest request) {
        return ResponseEntity.ok(serviceService.updateService(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a service - Admin only")
    public ResponseEntity<Void> deleteService(@PathVariable UUID id) {
        serviceService.deleteService(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search/advanced")
    @Operation(summary = "Advanced search services with pagination and filtering")
    public ResponseEntity<Page<AdminServiceResponse>> searchServicesAdvanced(
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

        return ResponseEntity.ok(serviceService.searchServicesForAdmin(criteria, pagination));
    }
}