package petitus.petcareplus.dto.request.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ServiceProviderProfileRequest extends ProfileRequest {
    private String contactPhone;

    private String contactEmail;

    private Map<String, Object> availableTime;

    private Set<String> skills;

    private Set<String> imageUrls;

}
