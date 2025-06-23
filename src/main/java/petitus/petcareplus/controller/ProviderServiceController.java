package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.dto.request.service.ProviderServicePatchRequest;
import petitus.petcareplus.dto.request.service.ProviderServiceRequest;
import petitus.petcareplus.dto.response.service.ProviderServiceResponse;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.ProviderServiceCriteria;
import petitus.petcareplus.service.ProviderServiceService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
//import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/provider-services")
@Tag(name = "Provider Services", description = "APIs for managing provider services")
@SecurityRequirement(name = "bearerAuth")
public class ProviderServiceController {
    private final ProviderServiceService providerServiceService;

    @GetMapping
    @Operation(summary = "Get all provider services with pagination and filtering")
    public ResponseEntity<Page<ProviderServiceResponse>> getAllProviderServices(
            // Search & Filter parameters
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID providerId,
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAtStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAtEnd,
            @RequestParam(required = false) Boolean isDeleted,

            // Pagination parameters
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sort) {
        // Build criteria từ request parameters
        ProviderServiceCriteria criteria = ProviderServiceCriteria.builder()
                .query(query)
                .providerId(providerId)
                .serviceId(serviceId)
                .minCustomPrice(minPrice)
                .maxCustomPrice(maxPrice)
                // .location(location)
                .createdAtStart(createdAtStart)
                .createdAtEnd(createdAtEnd)
                .isDeleted(isDeleted)
                .build();

        // Build pagination từ request parameters
        PaginationCriteria pagination = PaginationCriteria.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sort(sort)
                .columns(new String[] { "customPrice", "createdAt", "updatedAt" }) // Allowed sort columns
                .build();

        return ResponseEntity.ok(providerServiceService.getAllProviderServices(criteria, pagination));
    }
    // @GetMapping
    // @Operation(summary = "Get all provider services")
    // public ResponseEntity<List<ProviderServiceResponse>> getAllProviderServices()
    // {
    // return ResponseEntity.ok(providerServiceService.getAllProviderServices());
    // }

    @GetMapping("/{id}")
    @Operation(summary = "Get a provider service by ID")
    public ResponseEntity<ProviderServiceResponse> getProviderServiceById(@PathVariable UUID id) {
        return ResponseEntity.ok(providerServiceService.getProviderServiceById(id));
    }

    // @GetMapping("/provider/{providerId}")
    // @Operation(summary = "Get all services offered by a provider")
    // public ResponseEntity<List<ProviderServiceResponse>>
    // getProviderServices(@PathVariable UUID providerId) {
    // return
    // ResponseEntity.ok(providerServiceService.getProviderServices(providerId));
    // }

    // @GetMapping("/service/{serviceId}")
    // @Operation(summary = "Get all providers offering a service")
    // public ResponseEntity<List<ProviderServiceResponse>>
    // getProvidersByService(@PathVariable UUID serviceId) {
    // return
    // ResponseEntity.ok(providerServiceService.getProvidersByService(serviceId));
    // }

    @PostMapping
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    @Operation(summary = "Add a service to provider's offerings")
    public ResponseEntity<ProviderServiceResponse> addServiceToProvider(
            @Valid @RequestBody ProviderServiceRequest request) {
        return new ResponseEntity<>(
                providerServiceService.addServiceToProvider(request),
                HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    @Operation(summary = "Update a provider's service offering")
    public ResponseEntity<ProviderServiceResponse> updateProviderService(
            @PathVariable UUID id,
            @Valid @RequestBody ProviderServicePatchRequest request) {
        return ResponseEntity.ok(
                providerServiceService.updateProviderService(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    @Operation(summary = "Remove a service from provider's offerings")
    public ResponseEntity<Void> removeServiceFromProvider(
            @PathVariable UUID id) {
        providerServiceService.removeServiceFromProvider(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/provider/{providerId}/service/{serviceId}")
    @Operation(summary = "Get specific provider service by provider and service IDs")
    public ResponseEntity<ProviderServiceResponse> getProviderServiceByProviderAndService(
            @PathVariable UUID providerId,
            @PathVariable UUID serviceId) {
        return ResponseEntity.ok(
                providerServiceService.getProviderServiceByProviderAndService(providerId, serviceId));
    }
}