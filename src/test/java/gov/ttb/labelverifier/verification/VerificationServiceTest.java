package gov.ttb.labelverifier.verification;

import gov.ttb.labelverifier.ocr.ExtractedLabelText;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationServiceTest {
    private final VerificationService service = new VerificationService();

    @Test
    void passesMatchingLabelWithNormalizedBrandAndAbv() {
        String text = "Stone’s   Throw\n45 % Alc./Vol.\n" + VerificationService.GOVERNMENT_WARNING;

        VerificationResult result = service.verify(
                new LabelApplication("STONE'S THROW", new BigDecimal("45.00")), new ExtractedLabelText(text), 18);

        assertThat(result.status()).isEqualTo(VerificationStatus.PASS);
        assertThat(result.fields()).allMatch(field -> field.status() == VerificationStatus.PASS);
    }

    @Test
    void reviewsBrandMismatch() {
        VerificationResult result = verify("SOME OTHER BRAND\n45%\n" + VerificationService.GOVERNMENT_WARNING);

        assertThat(result.status()).isEqualTo(VerificationStatus.REVIEW);
        assertThat(field(result, "Brand name").detected()).isEqualTo("Not found");
    }

    @Test
    void reviewsAbvMismatchAndReportsDetectedValue() {
        VerificationResult result = verify("OLD TOM DISTILLERY\n40% ALC BY VOL\n" + VerificationService.GOVERNMENT_WARNING);

        FieldResult abv = field(result, "Alcohol by volume");
        assertThat(abv.status()).isEqualTo(VerificationStatus.REVIEW);
        assertThat(abv.detected()).isEqualTo("40%");
    }

    @Test
    void reviewsAmbiguousAbvValues() {
        VerificationResult result = verify("OLD TOM DISTILLERY\n45% ALC./VOL.\n40% ALC./VOL.\n" + VerificationService.GOVERNMENT_WARNING);

        FieldResult abv = field(result, "Alcohol by volume");
        assertThat(abv.status()).isEqualTo(VerificationStatus.REVIEW);
        assertThat(abv.explanation()).contains("Multiple ABV values");
    }

    @Test
    void reviewsMissingAbv() {
        VerificationResult result = verify("OLD TOM DISTILLERY\n" + VerificationService.GOVERNMENT_WARNING);

        assertThat(field(result, "Alcohol by volume").explanation()).contains("No ABV");
    }

    @Test
    void reviewsTitleCaseWarningHeading() {
        String warning = VerificationService.GOVERNMENT_WARNING.replace("GOVERNMENT WARNING:", "Government Warning:");
        VerificationResult result = verify("OLD TOM DISTILLERY\n45%\n" + warning);

        FieldResult warningResult = field(result, "Government warning");
        assertThat(warningResult.status()).isEqualTo(VerificationStatus.REVIEW);
        assertThat(warningResult.explanation()).contains("uppercase");
    }

    @Test
    void reviewsWarningWithMissingWord() {
        String warning = VerificationService.GOVERNMENT_WARNING.replace("HEALTH PROBLEMS", "PROBLEMS");
        VerificationResult result = verify("OLD TOM DISTILLERY\n45%\n" + warning);

        assertThat(field(result, "Government warning").explanation()).contains("differs");
    }

    private VerificationResult verify(String text) {
        return service.verify(new LabelApplication("OLD TOM DISTILLERY", new BigDecimal("45")),
                new ExtractedLabelText(text), 10);
    }

    private FieldResult field(VerificationResult result, String name) {
        return result.fields().stream().filter(field -> field.field().equals(name)).findFirst().orElseThrow();
    }
}
