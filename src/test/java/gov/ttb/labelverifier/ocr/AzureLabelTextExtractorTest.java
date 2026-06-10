package gov.ttb.labelverifier.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.ttb.labelverifier.config.OcrProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AzureLabelTextExtractorTest {
    private final AzureLabelTextExtractor extractor = new AzureLabelTextExtractor(RestClient.builder(), new ObjectMapper(),
            new OcrProperties("azure", "https://example.test", "test-key", Duration.ofSeconds(1)));

    @Test
    void mapsReadLinesIntoPlainText() {
        String response = """
                {"readResult":{"blocks":[{"lines":[{"text":"OLD TOM DISTILLERY"},{"text":"45% ALC./VOL."}]}]}}
                """;

        assertThat(extractor.parseLines(response)).isEqualTo("OLD TOM DISTILLERY\n45% ALC./VOL.");
    }

    @Test
    void rejectsResponseWithoutReadableLines() {
        assertThatThrownBy(() -> extractor.parseLines("{\"readResult\":{\"blocks\":[]}}"))
                .isInstanceOf(OcrException.class).hasMessageContaining("No readable text");
    }

    @Test
    void rejectsMalformedResponse() {
        assertThatThrownBy(() -> extractor.parseLines("not-json"))
                .isInstanceOf(OcrException.class).hasMessageContaining("unreadable response");
    }
}
