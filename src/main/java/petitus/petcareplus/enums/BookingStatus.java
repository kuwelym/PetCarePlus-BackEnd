package petitus.petcareplus.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BookingStatus {
    PENDING("pending"),
    ACCEPTED("accepted"),
    ONGOING("ongoing"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    private final String value;

    BookingStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BookingStatus fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (BookingStatus status : BookingStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Invalid booking status: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}