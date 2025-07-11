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
public class ServiceProviderProfileRequest {
    private String businessName;

    private String businessBio;

    private String businessAddress;

    private String contactPhone;

    private String contactEmail;

    private Map<String, Object> availableTime;

    private Set<String> imageUrls;

}
