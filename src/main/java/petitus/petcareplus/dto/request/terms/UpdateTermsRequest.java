package petitus.petcareplus.dto.request.terms;

import lombok.Data;
import petitus.petcareplus.utils.enums.TermsType;

@Data
public class UpdateTermsRequest {
    private TermsType type;
    private String language;
    private String title;
    private String content;
    private String version;
    private boolean isActive;
}
