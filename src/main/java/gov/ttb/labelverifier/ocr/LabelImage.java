package gov.ttb.labelverifier.ocr;

public record LabelImage(byte[] bytes, String contentType, String originalFilename) {
    public LabelImage {
        bytes = bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
