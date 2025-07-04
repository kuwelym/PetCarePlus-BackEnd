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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final UserService userService;

    @Transactional
    public PetResponse createPet(CreatePetRequest request) {
        UUID userId = userService.getCurrentUserId();

        Pet pet = Pet.builder()
                .userId(userId)
                .name(request.getName())
                .age(request.getAge())
                .dayOfBirth(request.getDayOfBirth())
                .species(request.getSpecies())
                .breed(request.getBreed())
                .gender(request.getGender())
                .size(request.getSize())
                .imageUrl(request.getImageUrl())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        petRepository.save(pet);
        return convertToResponse(pet);
    }

    public List<PetResponse> getAllPetsByUser() {
        UUID userId = userService.getCurrentUserId();

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
    public PetResponse updatePet(UUID petId, UpdatePetRequest request) {
        UUID userId = userService.getCurrentUserId();

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));
        if (!pet.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Pet does not belong to the user");
        }

        if (request.getName() != null) pet.setName(request.getName());
        if (request.getAge() != null) pet.setAge(request.getAge());
        if (request.getSpecies() != null) pet.setSpecies(request.getSpecies());
        if (request.getBreed() != null) pet.setBreed(request.getBreed());
        if (request.getGender() != null) pet.setGender(request.getGender());
        if (request.getSize() != null) pet.setSize(request.getSize());
        if (request.getDescription() != null) pet.setDescription(request.getDescription());
        if (request.getImageUrl() != null) pet.setImageUrl(request.getImageUrl());

        pet.setUpdatedAt(LocalDateTime.now());
        petRepository.save(pet);
        return convertToResponse(pet);
    }

    @Transactional
    public void deletePet(UUID petId) {
        UUID userId = userService.getCurrentUserId();
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
                .dayOfBirth(pet.getDayOfBirth())
                .species(pet.getSpecies())
                .breed(pet.getBreed())
                .gender(pet.getGender())
                .size(pet.getSize())
                .description(pet.getDescription())
                .imageUrl(pet.getImageUrl())
                .createdAt(pet.getCreatedAt())
                .updatedAt(pet.getUpdatedAt())
                .deletedAt(pet.getDeletedAt())
                .build();
    }
}
