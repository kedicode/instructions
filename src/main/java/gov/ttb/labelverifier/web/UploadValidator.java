package gov.ttb.labelverifier.web;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

@Component
public class UploadValidator {
    static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> SUPPORTED_TYPES = Set.of("image/jpeg", "image/png");

    public byte[] validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidUploadException("Choose a JPG or PNG label image.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidUploadException("The image must be 5 MB or smaller.");
        }
        if (!SUPPORTED_TYPES.contains(file.getContentType())) {
            throw new InvalidUploadException("Only JPG and PNG images are supported.");
        }
        try {
            byte[] bytes = file.getBytes();
            if (ImageIO.read(new ByteArrayInputStream(bytes)) == null) {
                throw new InvalidUploadException("The uploaded file is not a readable JPG or PNG image.");
            }
            return bytes;
        } catch (IOException exception) {
            throw new InvalidUploadException("The uploaded image could not be read.");
        }
    }
}
