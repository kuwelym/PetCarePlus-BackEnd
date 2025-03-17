package petitus.petcareplus.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.media.Schema;

public enum BookingStatus {
    @Schema(description = "Booking has been created but not yet accepted by provider")
    PENDING("pending"),

    @Schema(description = "Booking has been accepted by the provider")
    ACCEPTED("accepted"),

    @Schema(description = "Service is currently being provided")
    ONGOING("ongoing"),

    @Schema(description = "Service has been completed")
    COMPLETED("completed"),

    @Schema(description = "Booking has been cancelled")
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