package gov.ttb.labelverifier.web;

public class InvalidUploadException extends RuntimeException {
    public InvalidUploadException(String message) {
        super(message);
    }
}
