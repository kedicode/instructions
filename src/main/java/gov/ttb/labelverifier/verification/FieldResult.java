package gov.ttb.labelverifier.verification;

public record FieldResult(
        String field,
        String expected,
        String detected,
        VerificationStatus status,
        String explanation) {
}
