package petitus.petcareplus.controller;

import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.dto.request.profile.ProfileRequest;
import petitus.petcareplus.dto.request.profile.ServiceProviderProfileRequest;
import petitus.petcareplus.dto.response.PaginationResponse;
import petitus.petcareplus.dto.response.SuccessResponse;
import petitus.petcareplus.dto.response.profile.ProfileResponse;
import petitus.petcareplus.model.profile.Profile;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.ProfileCriteria;
import petitus.petcareplus.service.MessageSourceService;
import petitus.petcareplus.service.ProfileService;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profiles")
public class ProfileController extends BaseController {
    private final String[] SORT_COLUMNS = new String[]{"id", "rating", "dob", "gender", "createdAt", "updatedAt", "deletedAt"};
    private final ProfileService profileService;

    private final MessageSourceService messageSourceService;

    @PostMapping
    public ResponseEntity<SuccessResponse> createProfile(@RequestBody ProfileRequest profileRequest) {
        profileService.saveProfile(profileRequest);

        return ResponseEntity.ok(SuccessResponse.builder()
                .message(messageSourceService.get("profile_created"))
                .build());
    }

    @PostMapping("/service-provider")
    public ResponseEntity<SuccessResponse> createServiceProviderProfile(@RequestBody ServiceProviderProfileRequest serviceProviderProfileRequest) {
        profileService.saveServiceProviderProfile(serviceProviderProfileRequest);

        return ResponseEntity.ok(SuccessResponse.builder()
                .message(messageSourceService.get("profile_created"))
                .build());
    }

    @GetMapping
    public ResponseEntity<PaginationResponse<ProfileResponse>> list(
            @RequestParam(required = false) final String query,

            @RequestParam(required = false) final Boolean isServiceProvider,

            @RequestParam(required = false) final String location,


            @RequestParam(required = false) final int rating,

            @RequestParam(required = false) final List<String> skills,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime availableAtStart,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime availableAtEnd,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime createdAtStart,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime createdAtEnd,

            @RequestParam(defaultValue = "1", required = false) final Integer page,

            @RequestParam(defaultValue = "10", required = false) final Integer size,
            @RequestParam(defaultValue = "createdAt", required = false) final String sortBy,

            @RequestParam(defaultValue = "asc", required = false) @Pattern(regexp = "asc|desc") final String sort
    ) {

        sortColumnCheck(messageSourceService, SORT_COLUMNS, sortBy);

        Page<Profile> profiles = profileService.findAll(
                ProfileCriteria.builder()
                        .query(query)
                        .isServiceProvider(isServiceProvider)
                        .location(location)
                        .rating(rating)
                        .skills(skills)
                        .availableAtStart(availableAtStart)
                        .availableAtEnd(availableAtEnd)
                        .createdAtStart(createdAtStart)
                        .createdAtEnd(createdAtEnd)
                        .build(),
                PaginationCriteria.builder()
                        .page(page)
                        .size(size)
                        .sortBy(sortBy)
                        .sort(sort)
                        .columns(SORT_COLUMNS)
                        .build());

        return ResponseEntity.ok(new PaginationResponse<>(profiles, profiles.getContent().stream()
                .map(ProfileResponse::convert)
                .collect(toList())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable String id) {
        return ResponseEntity.ok(ProfileResponse.convert(profileService.findById(id)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ProfileResponse> getProfileByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(ProfileResponse.convert(profileService.findByUserId(userId)));
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile() {
        return ResponseEntity.ok(ProfileResponse.convert(profileService.getMyProfile()));
    }
}
