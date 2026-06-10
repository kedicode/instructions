package gov.ttb.labelverifier.verification;

import gov.ttb.labelverifier.ocr.ExtractedLabelText;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VerificationService {
    public static final String GOVERNMENT_WARNING = "GOVERNMENT WARNING: (1) ACCORDING TO THE SURGEON GENERAL, "
            + "WOMEN SHOULD NOT DRINK ALCOHOLIC BEVERAGES DURING PREGNANCY BECAUSE OF THE RISK OF BIRTH DEFECTS. "
            + "(2) CONSUMPTION OF ALCOHOLIC BEVERAGES IMPAIRS YOUR ABILITY TO DRIVE A CAR OR OPERATE MACHINERY, "
            + "AND MAY CAUSE HEALTH PROBLEMS.";

    private static final Pattern ABV_PATTERN = Pattern.compile(
            "(?i)(?:ALC(?:OHOL)?\\.?\\s*)?(\\d{1,2}(?:\\.\\d+)?)\\s*%\\s*(?:ALC(?:OHOL)?\\.?\\s*(?:/|BY)?\\s*VOL(?:UME)?\\.?)?");

    public VerificationResult verify(LabelApplication application, ExtractedLabelText extracted, long processingTimeMillis) {
        String text = extracted.text();
        List<FieldResult> fields = new ArrayList<>();
        fields.add(verifyBrand(application.brandName(), text));
        fields.add(verifyAbv(application.abv(), text));
        fields.add(verifyWarning(text));
        VerificationStatus status = fields.stream().allMatch(field -> field.status() == VerificationStatus.PASS)
                ? VerificationStatus.PASS : VerificationStatus.REVIEW;
        return new VerificationResult(status, List.copyOf(fields), text, processingTimeMillis);
    }

    private FieldResult verifyBrand(String expected, String text) {
        String normalizedExpected = TextNormalizer.normalize(expected);
        String normalizedText = TextNormalizer.normalize(text);
        boolean match = !normalizedExpected.isBlank() && normalizedText.contains(normalizedExpected);
        return new FieldResult("Brand name", expected, match ? expected : "Not found",
                match ? VerificationStatus.PASS : VerificationStatus.REVIEW,
                match ? "The brand name appears on the label, ignoring case and typographic apostrophe differences."
                        : "The expected brand name was not found in the OCR text. A reviewer should compare the artwork.");
    }

    private FieldResult verifyAbv(BigDecimal expected, String text) {
        Matcher matcher = ABV_PATTERN.matcher(text);
        List<BigDecimal> values = new ArrayList<>();
        while (matcher.find()) {
            values.add(new BigDecimal(matcher.group(1)).stripTrailingZeros());
        }
        BigDecimal normalizedExpected = expected.stripTrailingZeros();
        List<BigDecimal> distinctValues = values.stream().distinct().toList();
        boolean match = distinctValues.size() == 1 && distinctValues.getFirst().compareTo(normalizedExpected) == 0;
        String detected = distinctValues.isEmpty() ? "Not found" : distinctValues.stream().map(BigDecimal::toPlainString)
                .map(value -> value + "%").reduce((left, right) -> left + ", " + right).orElse("Not found");
        String explanation;
        if (match) {
            explanation = "The expected ABV was found and matched numerically.";
        } else if (distinctValues.isEmpty()) {
            explanation = "No ABV could be read from the label.";
        } else if (distinctValues.size() > 1) {
            explanation = "Multiple ABV values were detected. A reviewer should identify the value that applies.";
        } else {
            explanation = "The detected ABV does not match the application.";
        }
        return new FieldResult("Alcohol by volume", normalizedExpected.toPlainString() + "%", detected,
                match ? VerificationStatus.PASS : VerificationStatus.REVIEW, explanation);
    }

    private FieldResult verifyWarning(String text) {
        String normalizedText = TextNormalizer.normalize(text);
        String normalizedWarning = TextNormalizer.normalize(GOVERNMENT_WARNING);
        boolean heading = text.contains("GOVERNMENT WARNING:");
        boolean exact = heading && normalizedText.contains(normalizedWarning);
        String explanation;
        if (exact) {
            explanation = "The required heading and warning wording were found. Bold styling is not verified by this MVP.";
        } else if (!heading) {
            explanation = "The exact uppercase GOVERNMENT WARNING: heading was not found.";
        } else {
            explanation = "The heading was found, but the warning body is missing or differs from the canonical wording.";
        }
        return new FieldResult("Government warning", "Canonical warning text", exact ? "Complete text found" : "Exact text not found",
                exact ? VerificationStatus.PASS : VerificationStatus.REVIEW, explanation);
    }
}
