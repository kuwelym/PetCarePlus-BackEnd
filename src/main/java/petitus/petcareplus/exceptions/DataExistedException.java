package petitus.petcareplus.exceptions;

public class DataExistedException extends RuntimeException{
    public DataExistedException(String message) {
        super(message);
    }

    public DataExistedException() {
        super("Data existed");
    }
}
