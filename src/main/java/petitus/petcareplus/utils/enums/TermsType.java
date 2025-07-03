package petitus.petcareplus.utils.enums;

public enum TermsType {
    USER_TERMS("User Terms & Conditions"),
    PROVIDER_TERMS("Service Provider Terms & Conditions"),
    PRIVACY_POLICY("Privacy Policy"),
    PAYMENT_TERMS("Payment Terms");

    private final String displayName;

    TermsType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}