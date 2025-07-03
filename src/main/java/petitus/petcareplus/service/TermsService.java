package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import petitus.petcareplus.dto.response.terms.TermsResponse;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.TermsAndConditions;
import petitus.petcareplus.repository.TermsRepository;
import petitus.petcareplus.utils.enums.TermsType;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TermsService {

    private final TermsRepository termsRepository;

    public TermsResponse getTermsByType(TermsType type, String language) {
        TermsAndConditions terms = termsRepository
                .findByTypeAndLanguageAndIsActiveTrue(type, language)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Terms not found for type: " + type + " and language: " + language));

        return mapToResponse(terms);
    }

    public List<TermsResponse> getAllTerms(String language) {
        List<TermsAndConditions> termsList = termsRepository
                .findByLanguageAndIsActiveTrueOrderByCreatedAtDesc(language);

        return termsList.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TermsResponse> getAllTermsAllLanguages() {
        List<TermsAndConditions> termsList = termsRepository
                .findByIsActiveTrueOrderByTypeAscLanguageAsc();

        return termsList.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TermsResponse mapToResponse(TermsAndConditions terms) {
        return TermsResponse.builder()
                .id(terms.getId())
                .type(terms.getType())
                .language(terms.getLanguage())
                .version(terms.getVersion())
                .title(terms.getTitle())
                .content(terms.getContent())
                .createdAt(terms.getCreatedAt())
                .updatedAt(terms.getUpdatedAt())
                .build();
    }
}