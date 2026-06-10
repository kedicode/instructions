package gov.ttb.labelverifier.ocr;

public record ExtractedLabelText(String text) {
    public ExtractedLabelText {
        text = text == null ? "" : text;
    }
}
