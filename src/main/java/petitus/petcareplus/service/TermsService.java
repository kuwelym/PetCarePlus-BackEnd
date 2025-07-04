package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import petitus.petcareplus.dto.request.terms.CreateTermsRequest;
import petitus.petcareplus.dto.request.terms.UpdateTermsRequest;
import petitus.petcareplus.dto.response.terms.AdminTermsResponse;
import petitus.petcareplus.dto.response.terms.TermsResponse;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.TermsAndConditions;
import petitus.petcareplus.repository.TermsRepository;
import petitus.petcareplus.utils.enums.TermsType;

import java.util.List;
import java.util.UUID;
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

        private AdminTermsResponse mapToAdminResponse(TermsAndConditions terms) {
                return AdminTermsResponse.builder()
                                .id(terms.getId())
                                .type(terms.getType())
                                .language(terms.getLanguage())
                                .version(terms.getVersion())
                                .title(terms.getTitle())
                                .content(terms.getContent())
                                .isActive(terms.getIsActive())
                                .createdAt(terms.getCreatedAt())
                                .updatedAt(terms.getUpdatedAt())
                                .build();
        }

        @Transactional
        public TermsResponse createTerms(CreateTermsRequest request) {
                try {
                        TermsAndConditions existingTerms = termsRepository
                                        .findByTypeAndLanguageAndIsActiveTrue(request.getType(), request.getLanguage())
                                        .orElse(null);
                        if (existingTerms != null) {
                                throw new IllegalArgumentException("Terms already exist for type: "
                                                + request.getType() + " and language: " + request.getLanguage());
                        }
                        TermsAndConditions terms = TermsAndConditions.builder()
                                        .type(request.getType())
                                        .language(request.getLanguage())
                                        .title(request.getTitle())
                                        .content(request.getContent())
                                        .version("1.0") // Default version, can be updated later
                                        .isActive(true) // Default to active
                                        .build();

                        TermsAndConditions savedTerms = termsRepository.save(terms);
                        return mapToResponse(savedTerms);

                } catch (IllegalArgumentException e) {
                        throw new ResourceNotFoundException(e.getMessage());
                }
        }

        @Transactional
        public TermsResponse updateTerms(UUID id, UpdateTermsRequest request) {
                TermsAndConditions existingTerms = termsRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Terms not found with ID: " + id));

                existingTerms.setType(request.getType());
                existingTerms.setLanguage(request.getLanguage());
                existingTerms.setTitle(request.getTitle());
                existingTerms.setContent(request.getContent());
                existingTerms.setVersion(request.getVersion());

                TermsAndConditions updatedTerms = termsRepository.save(existingTerms);
                return mapToResponse(updatedTerms);
        }

        public AdminTermsResponse getTermsByTypeForAdmin(TermsType type, String language) {
                TermsAndConditions terms = termsRepository
                                .findByTypeAndLanguageAndIsActiveTrue(type, language)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Terms not found for type: " + type + " and language: " + language));

                return mapToAdminResponse(terms);
        }

        public List<AdminTermsResponse> getAllTermsForAdmin(String language) {
                List<TermsAndConditions> termsList = termsRepository
                                .findByLanguageAndIsActiveTrueOrderByCreatedAtDesc(language);

                return termsList.stream()
                                .map(this::mapToAdminResponse)
                                .collect(Collectors.toList());
        }

        public List<AdminTermsResponse> getAllTermsAllLanguagesForAdmin() {
                List<TermsAndConditions> termsList = termsRepository
                                .findByIsActiveTrueOrderByTypeAscLanguageAsc();

                return termsList.stream()
                                .map(this::mapToAdminResponse)
                                .collect(Collectors.toList());
        }
}