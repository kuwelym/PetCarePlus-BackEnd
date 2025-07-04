package petitus.petcareplus.controller.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import petitus.petcareplus.dto.request.terms.CreateTermsRequest;
import petitus.petcareplus.dto.request.terms.UpdateTermsRequest;
import petitus.petcareplus.dto.response.terms.AdminTermsResponse;
import petitus.petcareplus.dto.response.terms.TermsResponse;
import petitus.petcareplus.service.TermsService;
import petitus.petcareplus.utils.enums.TermsType;

@RestController
@RequestMapping("/admin/terms")
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin")
@RequiredArgsConstructor
public class AdminTermsController {

    private final TermsService termsService;

    @PostMapping
    @Operation(summary = "Create terms - Admin only")
    public ResponseEntity<TermsResponse> createTerms(@Valid @RequestBody CreateTermsRequest request) {
        // Implementation for creating new terms
        return ResponseEntity.ok(termsService.createTerms(request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update terms - Admin only")
    public ResponseEntity<TermsResponse> updateTerms(@PathVariable UUID id,
            @Valid @RequestBody UpdateTermsRequest request) {
        // Implementation for updating terms
        return ResponseEntity.ok(termsService.updateTerms(id, request));
    }

    @GetMapping("/{type}")
    @Operation(summary = "Get terms by type", description = "Get terms and conditions by type and language")
    public ResponseEntity<AdminTermsResponse> getTermsByType(
            @Parameter(description = "Type of terms", example = "USER_TERMS") @PathVariable TermsType type,
            @Parameter(description = "Language code", example = "en") @RequestParam(defaultValue = "en") String language) {

        AdminTermsResponse terms = termsService.getTermsByTypeForAdmin(type, language);
        return ResponseEntity.ok(terms);
    }

    @GetMapping
    @Operation(summary = "Get all terms", description = "Get all terms and conditions for a specific language")
    public ResponseEntity<List<AdminTermsResponse>> getAllTerms(
            @Parameter(description = "Language code", example = "vi") @RequestParam(defaultValue = "vi") String language) {

        List<AdminTermsResponse> terms = termsService.getAllTermsForAdmin(language);
        return ResponseEntity.ok(terms);
    }

    @GetMapping("/all-languages")
    @Operation(summary = "Get all terms (all languages)", description = "Get all terms and conditions for all languages")
    public ResponseEntity<List<AdminTermsResponse>> getAllTermsAllLanguages() {
        List<AdminTermsResponse> terms = termsService.getAllTermsAllLanguagesForAdmin();
        return ResponseEntity.ok(terms);
    }
}
