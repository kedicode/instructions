package gov.ttb.labelverifier.verification;

import java.text.Normalizer;
import java.util.Locale;

final class TextNormalizer {
    private TextNormalizer() {
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}
