package gov.ttb.labelverifier.verification;

import java.util.List;

public record VerificationResult(
        VerificationStatus status,
        List<FieldResult> fields,
        String extractedText,
        long processingTimeMillis) {
}
