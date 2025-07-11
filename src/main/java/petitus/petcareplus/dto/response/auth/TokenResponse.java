package petitus.petcareplus.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class TokenResponse{
    private String token;

    private String refreshToken;

    private TokenExpirationResponse expiresIn;

    @JsonProperty("isServiceProvider")
    private boolean isServiceProvider;

    @Setter
    @Getter
    @Builder
    public static class TokenExpirationResponse {
        private long token;
        private long refreshToken;
    }
}
