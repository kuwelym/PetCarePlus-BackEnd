package petitus.petcareplus.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class SuccessResponse extends ResponseObject {
    private String message;
}