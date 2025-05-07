package petitus.petcareplus.utils.enums;

public enum BookingStatus {
    PENDING, ACCEPTED, ONGOING, SERVICE_DONE, COMPLETED, CANCELLED;
}

// public enum BookingStatus {
// @Schema(description = "Booking has been created but not yet accepted by
// provider")
// PENDING("pending"),

// @Schema(description = "Booking has been accepted by the provider")
// ACCEPTED("accepted"),

// @Schema(description = "Service is currently being provided")
// ONGOING("ongoing"),

// @Schema(description = "Service completed but not confirmed by tenant")
// SERVICE_DONE("service_done"),

// @Schema(description = "Service has been completed, tenant has confirmed the
// service")
// COMPLETED("completed"),

// @Schema(description = "Booking has been cancelled")
// CANCELLED("cancelled");

// private final String value;

// BookingStatus(String value) {
// this.value = value;
// }

// @JsonValue
// public String getValue() {
// return value;
// }

// @JsonCreator
// public static BookingStatus fromValue(String value) {
// if (value == null) {
// return null;
// }

// for (BookingStatus status : BookingStatus.values()) {
// if (status.value.equalsIgnoreCase(value)) {
// return status;
// }
// }

// throw new IllegalArgumentException("Invalid booking status: " + value);
// }

// @Override
// public String toString() {
// return value;
// }
// }