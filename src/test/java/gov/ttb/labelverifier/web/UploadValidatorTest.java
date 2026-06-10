package gov.ttb.labelverifier.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadValidatorTest {
    private final UploadValidator validator = new UploadValidator();

    @Test
    void acceptsReadablePng() throws Exception {
        byte[] png = png();
        MockMultipartFile file = new MockMultipartFile("labelImage", "label.png", "image/png", png);

        assertThat(validator.validate(file)).isEqualTo(png);
    }

    @Test
    void rejectsEmptyUpload() {
        MockMultipartFile file = new MockMultipartFile("labelImage", "", "image/png", new byte[0]);
        assertThatThrownBy(() -> validator.validate(file)).isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("Choose");
    }

    @Test
    void rejectsUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile("labelImage", "label.gif", "image/gif", new byte[]{1});
        assertThatThrownBy(() -> validator.validate(file)).isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("JPG and PNG");
    }

    @Test
    void rejectsContentThatIsNotAnImage() {
        MockMultipartFile file = new MockMultipartFile("labelImage", "label.png", "image/png", "not an image".getBytes());
        assertThatThrownBy(() -> validator.validate(file)).isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("not a readable");
    }

    @Test
    void rejectsOversizedUploadBeforeDecoding() {
        MockMultipartFile file = new MockMultipartFile("labelImage", "large.png", "image/png",
                new byte[(int) UploadValidator.MAX_FILE_SIZE + 1]);
        assertThatThrownBy(() -> validator.validate(file)).isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("5 MB");
    }

    static byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
