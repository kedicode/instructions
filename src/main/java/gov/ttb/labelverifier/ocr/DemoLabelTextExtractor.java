package gov.ttb.labelverifier.ocr;

import gov.ttb.labelverifier.verification.VerificationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ocr.mode", havingValue = "demo", matchIfMissing = true)
public class DemoLabelTextExtractor implements LabelTextExtractor {
    @Override
    public ExtractedLabelText extract(LabelImage image) {
        return new ExtractedLabelText("OLD TOM DISTILLERY\nKENTUCKY STRAIGHT BOURBON WHISKEY\n"
                + "45% ALC./VOL. (90 PROOF)\n750 mL\n" + VerificationService.GOVERNMENT_WARNING);
    }
}
