package gov.ttb.labelverifier.verification;

public enum VerificationStatus {
    PASS("Pass"),
    REVIEW("Review"),
    UNABLE_TO_PROCESS("Unable to process");

    private final String displayName;

    VerificationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
