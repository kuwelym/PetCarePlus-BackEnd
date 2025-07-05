package petitus.petcareplus.dto.response.terms;

import lombok.*;
import petitus.petcareplus.utils.enums.TermsType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermsResponse {
    private UUID id;
    private TermsType type;
    private String language;
    private String version;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}