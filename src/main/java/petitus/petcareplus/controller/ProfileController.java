package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.dto.request.profile.ProfileRequest;
import petitus.petcareplus.dto.response.PaginationResponse;
import petitus.petcareplus.dto.response.SuccessResponse;
import petitus.petcareplus.dto.response.profile.ProfileResponse;
import petitus.petcareplus.model.profile.Profile;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.ProfileCriteria;
import petitus.petcareplus.service.MessageSourceService;
import petitus.petcareplus.service.ProfileService;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profiles")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController extends BaseController {

    private final String[] SORT_COLUMNS = new String[]{"id", "dob", "gender", "createdAt", "updatedAt", "deletedAt"};
    private final ProfileService profileService;

    private final MessageSourceService messageSourceService;



    @PutMapping
    @Operation(
            tags = {"Profile"},
            summary = "Update profile",
            description = "API để cập nhật profile"
    )
    public ResponseEntity<SuccessResponse> updateProfile(@RequestBody ProfileRequest profileRequest) {
        profileService.updateProfile(profileRequest);

        return ResponseEntity.ok(SuccessResponse.builder()
                .message(messageSourceService.get("profile_updated"))
                .build());
    }



    @GetMapping
    @Operation(tags = {"Profile"}, summary = "Get all profiles", description = "API để lấy danh sách tất cả profile")
    public ResponseEntity<PaginationResponse<ProfileResponse>> list(
            @RequestParam(required = false) final String query,

            @RequestParam(required = false) final Boolean isServiceProvider,

            @RequestParam(required = false) final String location,

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
                        .location(location)
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
                .toList()));
    }

    @GetMapping("/{id}")
    @Operation(
            tags = {"Profile"},
            summary = "Get profile by ID",
            description = "API để lấy thông tin profile theo ID"
    )
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(ProfileResponse.convert(profileService.findById(id)));
    }

    @GetMapping("/user/{userId}")
    @Operation(
            tags = {"Profile"},
            summary = "Get profile by user ID",
            description = "API để lấy thông tin profile theo user ID"
    )
    public ResponseEntity<ProfileResponse> getProfileByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(ProfileResponse.convert(profileService.findByUserId(userId)));
    }

    @GetMapping("/me")
    @Operation(
            tags = {"Profile"},
            summary = "Get my profile",
            description = "API để lấy thông tin profile của tôi"
    )
    public ResponseEntity<ProfileResponse> getMyProfile() {
        return ResponseEntity.ok(ProfileResponse.convert(profileService.getMyProfile()));
    }
}
