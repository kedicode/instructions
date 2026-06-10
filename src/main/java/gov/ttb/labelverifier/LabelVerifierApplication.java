package gov.ttb.labelverifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LabelVerifierApplication {
    public static void main(String[] args) {
        SpringApplication.run(LabelVerifierApplication.class, args);
    }
}
