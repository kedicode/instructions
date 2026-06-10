package gov.ttb.labelverifier.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.ttb.labelverifier.config.OcrProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.ocr.mode", havingValue = "azure")
public class AzureLabelTextExtractor implements LabelTextExtractor {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OcrProperties properties;

    public AzureLabelTextExtractor(RestClient.Builder builder, ObjectMapper objectMapper, OcrProperties properties) {
        this.properties = properties;
        if (isBlank(properties.endpoint()) || isBlank(properties.key())) {
            throw new IllegalStateException("Azure OCR mode requires AZURE_VISION_ENDPOINT and AZURE_VISION_KEY.");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeout());
        requestFactory.setReadTimeout(properties.timeout());
        this.restClient = builder.requestFactory(requestFactory)
                .baseUrl(stripTrailingSlash(properties.endpoint())).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public ExtractedLabelText extract(LabelImage image) {
        try {
            String response = restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/computervision/imageanalysis:analyze")
                            .queryParam("api-version", "2024-02-01")
                            .queryParam("features", "read")
                            .build())
                    .header("Ocp-Apim-Subscription-Key", properties.key())
                    .contentType(MediaType.parseMediaType(image.contentType()))
                    .body(image.bytes())
                    .retrieve()
                    .body(String.class);
            return new ExtractedLabelText(parseLines(response));
        } catch (RestClientException exception) {
            throw new OcrException("Azure OCR could not process the image. Please retry or review the label manually.", exception);
        }
    }

    String parseLines(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode blocks = root.path("readResult").path("blocks");
            List<String> lines = new ArrayList<>();
            blocks.forEach(block -> block.path("lines").forEach(line -> {
                String text = line.path("text").asText();
                if (!text.isBlank()) {
                    lines.add(text);
                }
            }));
            if (lines.isEmpty()) {
                throw new OcrException("No readable text was found in the uploaded image.");
            }
            return String.join("\n", lines);
        } catch (OcrException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new OcrException("Azure OCR returned an unreadable response.", exception);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String stripTrailingSlash(String value) {
        return value.replaceFirst("/+$", "");
    }
}
