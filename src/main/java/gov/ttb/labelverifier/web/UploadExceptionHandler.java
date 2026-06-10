package gov.ttb.labelverifier.web;

import gov.ttb.labelverifier.config.OcrProperties;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class UploadExceptionHandler {
    private final OcrProperties properties;

    public UploadExceptionHandler(OcrProperties properties) {
        this.properties = properties;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String oversizedUpload(Model model) {
        model.addAttribute("form", new VerificationForm());
        model.addAttribute("uploadError", "The image must be 5 MB or smaller.");
        model.addAttribute("demoMode", !"azure".equalsIgnoreCase(properties.mode()));
        return "index";
    }
}
