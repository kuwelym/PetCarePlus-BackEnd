package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import petitus.petcareplus.dto.request.pet.AdminCreatePetRequest;
import petitus.petcareplus.dto.request.pet.AdminUpdatePetRequest;
import petitus.petcareplus.dto.request.pet.CreatePetRequest;
import petitus.petcareplus.dto.request.pet.UpdatePetRequest;
import petitus.petcareplus.dto.response.pet.AdminPetResponse;
import petitus.petcareplus.dto.response.pet.PetResponse;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.Pet;
import petitus.petcareplus.model.spec.PetSpecification;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.PetCriteria;
import petitus.petcareplus.repository.PetRepository;
import petitus.petcareplus.utils.PageRequestBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
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

        if (request.getName() != null)
            pet.setName(request.getName());
        if (request.getAge() != null)
            pet.setAge(request.getAge());
        if (request.getSpecies() != null)
            pet.setSpecies(request.getSpecies());
        if (request.getBreed() != null)
            pet.setBreed(request.getBreed());
        if (request.getGender() != null)
            pet.setGender(request.getGender());
        if (request.getDayOfBirth() != null)
            pet.setDayOfBirth(request.getDayOfBirth());
        if (request.getSize() != null)
            pet.setSize(request.getSize());
        if (request.getDescription() != null)
            pet.setDescription(request.getDescription());
        if (request.getImageUrl() != null)
            pet.setImageUrl(request.getImageUrl());

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

    private AdminPetResponse convertToAdminResponse(Pet pet) {
        return AdminPetResponse.builder()
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

    public Page<AdminPetResponse> getAllPetsForAdmin(PetCriteria criteria, PaginationCriteria pagination) {
        Specification<Pet> specification = new PetSpecification(criteria);
        PageRequest pageRequest = PageRequestBuilder.build(pagination);
        Page<Pet> pets = petRepository.findAll(specification, pageRequest);

        return pets.map(this::convertToAdminResponse);
    }

    public Page<AdminPetResponse> getPetsByUserId(UUID userId, PaginationCriteria pagination) {
        PageRequest pageRequest = PageRequestBuilder.build(pagination);
        Page<Pet> pets = petRepository.findByOwnerId(userId, pageRequest);

        return pets.map(this::convertToAdminResponse);
    }

    @Transactional
    public AdminPetResponse createPetForAdmin(AdminCreatePetRequest request) {
        // Admin có thể tạo pet cho bất kỳ user nào
        Pet pet = Pet.builder()
                .name(request.getName())
                .dayOfBirth(request.getDayOfBirth())
                .species(request.getSpecies())
                .breed(request.getBreed())
                .age(request.getAge())
                .gender(request.getGender())
                .size(request.getSize())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .userId(request.getOwnerId()) // Admin chỉ định owner
                .build();

        Pet savedPet = petRepository.save(pet);
        return convertToAdminResponse(savedPet);
    }

    @Transactional
    public AdminPetResponse updatePetForAdmin(UUID petId, AdminUpdatePetRequest request) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        if (request.getName() != null)
            pet.setName(request.getName());
        log.info("Updating pet name to: {}", request.getName());
        log.info("Current pet name", pet.getName());
        if (request.getAge() != null)
            pet.setAge(request.getAge());
        if (request.getSpecies() != null)
            pet.setSpecies(request.getSpecies());
        if (request.getBreed() != null)
            pet.setBreed(request.getBreed());
        if (request.getGender() != null)
            pet.setGender(request.getGender());
        if (request.getDayOfBirth() != null)
            pet.setDayOfBirth(request.getDayOfBirth());
        if (request.getSize() != null)
            pet.setSize(request.getSize());
        if (request.getDescription() != null)
            pet.setDescription(request.getDescription());
        if (request.getImageUrl() != null)
            pet.setImageUrl(request.getImageUrl());

        pet.setUpdatedAt(LocalDateTime.now());
        Pet updatedPet = petRepository.save(pet);
        return convertToAdminResponse(updatedPet);
    }

    @Transactional
    public void deletePetForAdmin(UUID petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        pet.setDeletedAt(LocalDateTime.now());
        petRepository.save(pet);
    }

    public Object getPetStatistics() {
        // Tạo thống kê pets
        long totalPets = petRepository.count();
        Map<String, Long> speciesCount = petRepository.countBySpecies();
        Map<String, Long> breedCount = petRepository.countByBreed();

        return Map.of(
                "totalPets", totalPets,
                "speciesDistribution", speciesCount,
                "breedDistribution", breedCount);
    }
}
