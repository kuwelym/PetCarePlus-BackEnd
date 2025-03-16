package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.dto.request.pet.CreatePetRequest;
import petitus.petcareplus.dto.request.pet.UpdatePetRequest;
import petitus.petcareplus.dto.response.pet.PetResponse;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.Pet;
import petitus.petcareplus.repository.PetRepository;
import petitus.petcareplus.security.jwt.JwtTokenProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final JwtTokenProvider jwtTokenProvider;

    private UUID extractUserId(String bearerToken) {
        String token = jwtTokenProvider.extractJwtFromBearerString(bearerToken);
        return UUID.fromString(jwtTokenProvider.getUserIdFromToken(token));
    }


    @Transactional
    public PetResponse createPet(String bearerToken, CreatePetRequest request) {
        UUID userId = extractUserId(bearerToken);

        Pet pet = Pet.builder()
                .userId(userId)
                .name(request.getName())
                .age(request.getAge())
                .species(request.getSpecies())
                .breed(request.getBreed())
                .hasChip(request.getHasChip())
                .vaccinated(request.getVaccinated())
                .imageUrl(request.getImageUrl())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        petRepository.save(pet);
        return convertToResponse(pet);
    }

    public List<PetResponse> getAllPetsByUser(String bearerToken) {
        UUID userId = extractUserId(bearerToken);

        return petRepository.findByUserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public PetResponse getPetById(UUID petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));
        return convertToResponse(pet);
    }

    @Transactional
    public PetResponse updatePet(String bearerToken, UUID petId, UpdatePetRequest request) {
        UUID userId = extractUserId(bearerToken);

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));
        if (!pet.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Pet does not belong to the user");
        }

        if (request.getName() != null) pet.setName(request.getName());
        if (request.getAge() != null) pet.setAge(request.getAge());
        if (request.getSpecies() != null) pet.setSpecies(request.getSpecies());
        if (request.getBreed() != null) pet.setBreed(request.getBreed());
        if (request.getHasChip() != null) pet.setHasChip(request.getHasChip());
        if (request.getVaccinated() != null) pet.setVaccinated(request.getVaccinated());
        if (request.getImageUrl() != null) pet.setImageUrl(request.getImageUrl());

        pet.setUpdatedAt(LocalDateTime.now());
        petRepository.save(pet);
        return convertToResponse(pet);
    }

    @Transactional
    public void deletePet(String bearerToken, UUID petId) {
        UUID userId = extractUserId(bearerToken);
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));
        if (!pet.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Pet does not belong to the user");
        }

        pet.setDeletedAt(LocalDateTime.now());
        petRepository.save(pet);
    }


    private PetResponse convertToResponse(Pet pet) {
        return PetResponse.builder()
                .id(pet.getId())
                .userId(pet.getUserId())
                .name(pet.getName())
                .age(pet.getAge())
                .species(pet.getSpecies())
                .breed(pet.getBreed())
                .hasChip(pet.getHasChip())
                .vaccinated(pet.getVaccinated())
                .imageUrl(pet.getImageUrl())
                .createdAt(pet.getCreatedAt())
                .updatedAt(pet.getUpdatedAt())
                .deletedAt(pet.getDeletedAt())
                .build();
    }
}
