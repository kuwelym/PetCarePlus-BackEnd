package petitus.petcareplus.exceptions;

public class BadRequestException extends RuntimeException{
    public BadRequestException(String message) {
        super(message);
    }

public BadRequestException() {
        super("Bad request");
    }
}
