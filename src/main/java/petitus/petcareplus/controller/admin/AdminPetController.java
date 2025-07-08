package petitus.petcareplus.controller.admin;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import petitus.petcareplus.dto.request.pet.AdminCreatePetRequest;
import petitus.petcareplus.dto.request.pet.AdminUpdatePetRequest;
import petitus.petcareplus.dto.response.PaginationResponse;
import petitus.petcareplus.dto.response.pet.AdminPetResponse;
import petitus.petcareplus.dto.response.pet.PetResponse;

import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.PetCriteria;
import petitus.petcareplus.service.PetService;

@RestController
@RequestMapping("/admin/pets")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class AdminPetController {

    private final PetService petService;

    @GetMapping
    @Operation(summary = "Get all pets with pagination and filtering")
    public ResponseEntity<PaginationResponse<AdminPetResponse>> getAllPets(

            // Search & Filter parameters
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) String gender,

            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sort) {

        PetCriteria criteria = PetCriteria.builder()
                .query(query)
                .species(species) // Filter by species
                .breed(breed) // Filter by breed
                .gender(gender) // Filter by gender
                .build();

        PaginationCriteria pagination = PaginationCriteria.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sort(sort)
                .columns(new String[] { "createdAt" }) // Allowed
                                                       // sort
                                                       // fields
                .build();

        Page<AdminPetResponse> pageResult = petService.getAllPetsForAdmin(criteria, pagination);

        // Convert sang PaginationResponse
        PaginationResponse<AdminPetResponse> response = new PaginationResponse<>(
                pageResult,
                pageResult.getContent());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get pet by ID")
    public ResponseEntity<PetResponse> getPetById(@PathVariable UUID id) {
        return ResponseEntity.ok(petService.getPetById(id));
    }

    @PostMapping("/add")
    @Operation(summary = "Add a new pet for admin")
    public ResponseEntity<AdminPetResponse> createPetForAdmin(@Valid @RequestBody AdminCreatePetRequest request) {
        AdminPetResponse response = petService.createPetForAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update pet details for admin")
    public ResponseEntity<AdminPetResponse> updatePetForAdmin(@PathVariable UUID id,
            @Valid @RequestBody AdminUpdatePetRequest request) {
        log.info("Request: {}", request);
        AdminPetResponse response = petService.updatePetForAdmin(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a pet for admin")
    public ResponseEntity<Void> deletePetForAdmin(@PathVariable UUID id) {
        petService.deletePetForAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get pet statistics")
    public ResponseEntity<Object> getPetStatistics() {
        return ResponseEntity.ok(petService.getPetStatistics());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all pets by user ID")
    public ResponseEntity<PaginationResponse<AdminPetResponse>> getPetsByUserIdForAdmin(@PathVariable UUID userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sort) {

        PaginationCriteria pagination = PaginationCriteria.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sort(sort)
                .columns(new String[] { "createdAt" }) // Allowed sort fields
                .build();

        Page<AdminPetResponse> pageResult = petService.getPetsByUserId(userId, pagination);

        // Convert sang PaginationResponse
        PaginationResponse<AdminPetResponse> response = new PaginationResponse<>(
                pageResult,
                pageResult.getContent());
        return ResponseEntity.ok(response);
    }

}
