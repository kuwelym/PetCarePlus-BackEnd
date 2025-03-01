package petitus.petcareplus.exceptions;

public class RefreshTokenExpireException extends RuntimeException {

    public RefreshTokenExpireException() {
        super("Refresh token has expired!");
    }

    public RefreshTokenExpireException(String message) {
        super(message);
    }
}
