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
@RequestMapping("")
@Tag(name = "Provider Services", description = "APIs for managing provider services")
public class ProviderServiceController {
    private final ProviderServiceService providerServiceService;

    @GetMapping("/provider-services")
    @Operation(summary = "Get all provider services")
    public ResponseEntity<List<ProviderServiceResponse>> getAllProviderServices() {
        return ResponseEntity.ok(providerServiceService.getAllProviderServices());
    }

    // @GetMapping("/providers/{providerId}/services")
    // @Operation(summary = "Get all services offered by a provider")
    // public ResponseEntity<List<ProviderServiceResponse>>
    // getProviderServices(@PathVariable UUID providerId) {
    // return
    // ResponseEntity.ok(providerServiceService.getProviderServices(providerId));
    // }

    // @GetMapping("/services/{serviceId}/providers")
    // @Operation(summary = "Get all providers offering a service")
    // public ResponseEntity<List<ProviderServiceResponse>>
    // getProvidersByService(@PathVariable UUID serviceId) {
    // return
    // ResponseEntity.ok(providerServiceService.getProvidersByService(serviceId));
    // }

    @PostMapping("/provider-services")
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

    @PatchMapping("/provider-services/{serviceId}")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    @Operation(summary = "Update a provider's service offering")
    public ResponseEntity<ProviderServiceResponse> updateProviderService(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @PathVariable UUID serviceId,
            @Valid @RequestBody ProviderServicePatchRequest request) {
        UUID providerId = currentUser.getId();
        return ResponseEntity.ok(
                providerServiceService.updateProviderService(providerId, serviceId, request));
    }

    @DeleteMapping("/provider-services/{serviceId}")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    @Operation(summary = "Remove a service from provider's offerings")
    public ResponseEntity<Void> removeServiceFromProvider(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @PathVariable UUID serviceId) {
        UUID providerId = currentUser.getId();
        providerServiceService.removeServiceFromProvider(providerId, serviceId);
        return ResponseEntity.noContent().build();
    }
}