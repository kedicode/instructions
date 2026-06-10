package gov.ttb.labelverifier.web;

import gov.ttb.labelverifier.config.OcrProperties;
import gov.ttb.labelverifier.ocr.ExtractedLabelText;
import gov.ttb.labelverifier.ocr.LabelTextExtractor;
import gov.ttb.labelverifier.ocr.OcrException;
import gov.ttb.labelverifier.verification.VerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(VerificationController.class)
@Import({VerificationService.class, UploadValidator.class})
class VerificationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LabelTextExtractor extractor;

    @MockBean
    private OcrProperties properties;

    @Test
    void showsForm() throws Exception {
        when(properties.mode()).thenReturn("demo");
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Alcohol Label Verifier")));
    }

    @Test
    void rendersPassResultFromExtractor() throws Exception {
        when(properties.mode()).thenReturn("demo");
        when(extractor.extract(any())).thenReturn(new ExtractedLabelText(
                "OLD TOM DISTILLERY\n45%\n" + VerificationService.GOVERNMENT_WARNING));

        mockMvc.perform(multipart("/verify").file(validPng()).param("brandName", "OLD TOM DISTILLERY").param("abv", "45"))
                .andExpect(status().isOk())
                .andExpect(view().name("result"))
                .andExpect(model().attributeExists("result"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("All automated checks passed")));
    }

    @Test
    void returnsToFormForInvalidImage() throws Exception {
        when(properties.mode()).thenReturn("demo");
        MockMultipartFile file = new MockMultipartFile("labelImage", "fake.png", "image/png", "bad".getBytes());

        mockMvc.perform(multipart("/verify").file(file).param("brandName", "OLD TOM").param("abv", "45"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("uploadError"));
    }

    @Test
    void rendersFriendlyOcrError() throws Exception {
        when(properties.mode()).thenReturn("azure");
        when(extractor.extract(any())).thenThrow(new OcrException("OCR is temporarily unavailable."));

        mockMvc.perform(multipart("/verify").file(validPng()).param("brandName", "OLD TOM").param("abv", "45"))
                .andExpect(status().isOk())
                .andExpect(view().name("result"))
                .andExpect(model().attribute("processingError", "OCR is temporarily unavailable."))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Unable to process")));
    }

    private MockMultipartFile validPng() throws Exception {
        return new MockMultipartFile("labelImage", "label.png", "image/png", UploadValidatorTest.png());
    }
}
