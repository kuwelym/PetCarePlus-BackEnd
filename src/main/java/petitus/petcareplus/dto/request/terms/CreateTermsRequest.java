package petitus.petcareplus.dto.request.terms;

import lombok.Data;
import petitus.petcareplus.utils.enums.TermsType;

@Data
public class CreateTermsRequest {

    private TermsType type;
    private String language;
    private String title;
    private String content;
}
