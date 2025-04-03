package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import petitus.petcareplus.dto.request.service.ProviderServicePatchRequest;
import petitus.petcareplus.dto.request.service.ProviderServiceRequest;
import petitus.petcareplus.dto.response.service.ProviderServiceResponse;
import petitus.petcareplus.security.jwt.JwtUserDetails;
import petitus.petcareplus.service.ProviderServiceService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/provider-services")
@Tag(name = "Provider Services", description = "APIs for managing provider services")
public class ProviderServiceController {
    private final ProviderServiceService providerServiceService;

    @GetMapping
    @Operation(summary = "Get all provider services")
    public ResponseEntity<List<ProviderServiceResponse>> getAllProviderServices() {
        return ResponseEntity.ok(providerServiceService.getAllProviderServices());
    }

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
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Valid @RequestBody ProviderServiceRequest request) {
        UUID providerId = currentUser.getId();
        return new ResponseEntity<>(
                providerServiceService.addServiceToProvider(providerId, request),
                HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    @Operation(summary = "Update a provider's service offering")
    public ResponseEntity<ProviderServiceResponse> updateProviderService(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody ProviderServicePatchRequest request) {
        UUID providerId = currentUser.getId();
        return ResponseEntity.ok(
                providerServiceService.updateProviderService(id, providerId, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    @Operation(summary = "Remove a service from provider's offerings")
    public ResponseEntity<Void> removeServiceFromProvider(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @PathVariable UUID id) {
        UUID providerId = currentUser.getId();
        providerServiceService.removeServiceFromProvider(id, providerId);
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