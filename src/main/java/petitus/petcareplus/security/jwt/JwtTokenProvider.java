package petitus.petcareplus.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.JwtToken;
import petitus.petcareplus.service.JwtTokenService;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

import static petitus.petcareplus.utils.Constants.TOKEN_HEADER;
import static petitus.petcareplus.utils.Constants.TOKEN_TYPE;

@Service
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Getter
    @Value("${application.security.jwt.access-token.expiration}")
    private long tokenExpiresIn;

    @Getter
    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshTokenExpiresIn;

    private final JwtTokenService jwtTokenService;

    private final HttpServletRequest httpServletRequest;

    public Date getIssuedAt(final String token) {
        return extractClaims(token, Claims::getIssuedAt);
    }

    public Date getExpiration(final String token) {
        return extractClaims(token, Claims::getExpiration);
    }

    public String getUserIdFromToken(final String token) {
        return extractClaims(token, Claims::getSubject);
    }

    public <T> T extractClaims(final String token, final Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(
                Jwts.parser()
                        .verifyWith(getSecretKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
        );
    }

    public String extractJwtFromBearerString(final String bearer) {
        if (StringUtils.hasText(bearer) && bearer.startsWith(String.format("%s ", TOKEN_TYPE))) {
            return bearer.substring(TOKEN_TYPE.length() + 1);
        }

        return null;
    }

    public String extractJwtFromRequest(final HttpServletRequest request) {
        return extractJwtFromBearerString(request.getHeader(TOKEN_HEADER));
    }

    private boolean isTokenExpired(final String token) {
        return extractClaims(token, Claims::getExpiration).before(new Date());
    }

    /**
     * This prevents the refresh token from being validated as an access token
     */
    public boolean isRefreshToken(final String token) {
        try {
            return getExpiration(token).getTime() - getIssuedAt(token).getTime() == refreshTokenExpiresIn;
        } catch (JwtException e) {
            return false;
        }
    }

    // Modify the validateToken method to ensure it only validates access tokens
    public boolean validateToken(final String token) {
        return validateToken(token, true) && !isRefreshToken(token);
    }

    public boolean validateRefreshToken(final String token) {
        return validateToken(token, true);
    }

    public boolean validateToken(final String token, final boolean isHttp) {
        extractClaims(token, Claims::getExpiration);
        try {
            JwtToken jwtToken = jwtTokenService.findByTokenOrRefreshToken(token);
            if (jwtToken == null) {
                return false;
            }
        } catch (ResourceNotFoundException e) {
            return false;
        }

        return !isTokenExpired(token);
    }

    public boolean validateToken(final String token, final HttpServletRequest httpServletRequest) {
        try {
            boolean isTokenValid = validateToken(token);
            if (!isTokenValid) {
                httpServletRequest.setAttribute("notfound", "Token is not found in cache");
            }
            return isTokenValid;
        } catch (UnsupportedJwtException e) {
            httpServletRequest.setAttribute("unsupported", "Unsupported JWT token!");
        } catch (MalformedJwtException e) {
            httpServletRequest.setAttribute("invalid", "Invalid JWT token!");
        } catch (ExpiredJwtException e) {
            httpServletRequest.setAttribute("expired", "Expired JWT token!");
        } catch (IllegalArgumentException e) {
            httpServletRequest.setAttribute("illegal", "JWT claims string is empty.");
        }

        return false;
    }

    public String generateTokenByUserId(final String userId, final Long expires) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expires))
                .signWith(getSecretKey())
                .compact();
    }

    public String generateToken(final String userId) {
        return generateTokenByUserId(userId, tokenExpiresIn);
    }

    public String generateRefreshToken(final String userId) {
        return generateTokenByUserId(userId, refreshTokenExpiresIn);
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}