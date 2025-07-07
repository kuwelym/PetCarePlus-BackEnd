package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.dto.response.terms.TermsResponse;
import petitus.petcareplus.service.TermsService;
import petitus.petcareplus.utils.enums.TermsType;

import java.util.List;

@RestController
@RequestMapping("/terms")
@RequiredArgsConstructor
@Tag(name = "Terms & Conditions", description = "APIs for terms and conditions")
public class TermsController {

    private final TermsService termsService;

    @GetMapping("/{type}")
    @Operation(summary = "Get terms by type", description = "Get terms and conditions by type and language")
    public ResponseEntity<TermsResponse> getTermsByType(
            @Parameter(description = "Type of terms", example = "USER_TERMS") @PathVariable TermsType type,
            @Parameter(description = "Language code", example = "en") @RequestParam(defaultValue = "en") String language) {

        TermsResponse terms = termsService.getTermsByType(type, language);
        return ResponseEntity.ok(terms);
    }

    @GetMapping
    @Operation(summary = "Get all terms", description = "Get all terms and conditions for a specific language")
    public ResponseEntity<List<TermsResponse>> getAllTerms(
            @Parameter(description = "Language code", example = "vi") @RequestParam(defaultValue = "vi") String language) {

        List<TermsResponse> terms = termsService.getAllTerms(language);
        return ResponseEntity.ok(terms);
    }

    @GetMapping("/all-languages")
    @Operation(summary = "Get all terms (all languages)", description = "Get all terms and conditions for all languages")
    public ResponseEntity<List<TermsResponse>> getAllTermsAllLanguages() {
        List<TermsResponse> terms = termsService.getAllTermsAllLanguages();
        return ResponseEntity.ok(terms);
    }
}