package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.dto.request.pet.CreatePetRequest;
import petitus.petcareplus.dto.request.pet.UpdatePetRequest;
import petitus.petcareplus.dto.response.pet.PetResponse;
import petitus.petcareplus.service.PetService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
@Tag(name = "Pets", description = "Các API quản lý thú cưng")
public class PetController {

    private final PetService petService;

    @PostMapping
    @Operation(summary = "Add a pet", description = "Thêm thú cưng mới", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PetResponse> addPet(@RequestHeader(value = "Authorization") String authorization,
                                              @RequestBody @Valid CreatePetRequest request) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.createPet(authorization, request));
    }

    @GetMapping
    @Operation(summary = "Get all pets", description = "Lấy danh sách thú cưng của người dùng", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<PetResponse>> getPets(@RequestHeader("Authorization") String authorization) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(petService.getAllPetsByUser(authorization));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a pet", description = "Lấy thông tin chi tiết một thú cưng")
    public ResponseEntity<PetResponse> getPet( @PathVariable UUID id) {
        return ResponseEntity.ok(petService.getPetById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update pet", description = "Cập nhật thông tin thú cưng", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PetResponse> updatePet(@RequestHeader("Authorization") String authorization, @PathVariable UUID id, @RequestBody UpdatePetRequest request) {
        return ResponseEntity.ok(petService.updatePet(authorization, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete pet", description = "Xóa thú cưng",security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deletePet(@RequestHeader("Authorization") String authorization, @PathVariable UUID id) {
        petService.deletePet(authorization, id);
        return ResponseEntity.noContent().build();
    }
}
