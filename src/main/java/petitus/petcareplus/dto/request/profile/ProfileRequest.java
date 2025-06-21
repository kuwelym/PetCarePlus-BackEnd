package petitus.petcareplus.dto.request.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProfileRequest {
    private String dob;
    private String avatarUrl;
    private String gender;
    private String location;
    private String about;
}
