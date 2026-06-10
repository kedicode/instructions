package gov.ttb.labelverifier.web;

import gov.ttb.labelverifier.config.OcrProperties;
import gov.ttb.labelverifier.ocr.ExtractedLabelText;
import gov.ttb.labelverifier.ocr.LabelImage;
import gov.ttb.labelverifier.ocr.LabelTextExtractor;
import gov.ttb.labelverifier.ocr.OcrException;
import gov.ttb.labelverifier.verification.LabelApplication;
import gov.ttb.labelverifier.verification.VerificationResult;
import gov.ttb.labelverifier.verification.VerificationService;
import gov.ttb.labelverifier.verification.VerificationStatus;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
public class VerificationController {
    private final LabelTextExtractor extractor;
    private final VerificationService verificationService;
    private final UploadValidator uploadValidator;
    private final OcrProperties ocrProperties;

    public VerificationController(LabelTextExtractor extractor, VerificationService verificationService,
                                  UploadValidator uploadValidator, OcrProperties ocrProperties) {
        this.extractor = extractor;
        this.verificationService = verificationService;
        this.uploadValidator = uploadValidator;
        this.ocrProperties = ocrProperties;
    }

    @GetMapping("/")
    public String index(Model model) {
        if (!model.containsAttribute("form")) {
            VerificationForm form = new VerificationForm();
            if (isDemoMode()) {
                form.setBrandName("OLD TOM DISTILLERY");
                form.setAbv(new java.math.BigDecimal("45"));
            }
            model.addAttribute("form", form);
        }
        addCommonAttributes(model);
        return "index";
    }

    @PostMapping("/verify")
    public String verify(@Valid @ModelAttribute("form") VerificationForm form, BindingResult bindingResult,
                         @RequestParam("labelImage") MultipartFile labelImage, Model model) {
        addCommonAttributes(model);
        byte[] bytes = null;
        try {
            bytes = uploadValidator.validate(labelImage);
        } catch (InvalidUploadException exception) {
            model.addAttribute("uploadError", exception.getMessage());
        }
        if (bindingResult.hasErrors() || bytes == null) {
            return "index";
        }

        long started = System.nanoTime();
        try {
            ExtractedLabelText extracted = extractor.extract(new LabelImage(bytes, labelImage.getContentType(), labelImage.getOriginalFilename()));
            long elapsed = (System.nanoTime() - started) / 1_000_000;
            VerificationResult result = verificationService.verify(
                    new LabelApplication(form.getBrandName(), form.getAbv()), extracted, elapsed);
            model.addAttribute("result", result);
        } catch (OcrException exception) {
            long elapsed = (System.nanoTime() - started) / 1_000_000;
            model.addAttribute("result", new VerificationResult(VerificationStatus.UNABLE_TO_PROCESS, List.of(), "", elapsed));
            model.addAttribute("processingError", exception.getMessage());
        }
        return "result";
    }

    private void addCommonAttributes(Model model) {
        model.addAttribute("demoMode", isDemoMode());
    }

    private boolean isDemoMode() {
        return !"azure".equalsIgnoreCase(ocrProperties.mode());
    }
}
