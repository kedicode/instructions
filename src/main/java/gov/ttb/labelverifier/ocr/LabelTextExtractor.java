package gov.ttb.labelverifier.ocr;

public interface LabelTextExtractor {
    ExtractedLabelText extract(LabelImage image);
}
