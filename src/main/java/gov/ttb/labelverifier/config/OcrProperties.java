package gov.ttb.labelverifier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.ocr")
public record OcrProperties(
        String mode,
        String endpoint,
        String key,
        Duration timeout) {
    public OcrProperties {
        mode = mode == null || mode.isBlank() ? "demo" : mode;
        timeout = timeout == null ? Duration.ofSeconds(4) : timeout;
    }
}
