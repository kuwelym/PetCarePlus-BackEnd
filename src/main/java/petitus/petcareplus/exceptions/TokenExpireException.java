package petitus.petcareplus.exceptions;

public class TokenExpireException extends RuntimeException {

    public TokenExpireException() {
        super("Token has expired!");
    }

    public TokenExpireException(String message) {
        super(message);
    }
}
