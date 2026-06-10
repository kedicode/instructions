package gov.ttb.labelverifier.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class VerificationForm {
    @NotBlank(message = "Enter the brand name from the application.")
    private String brandName;

    @NotNull(message = "Enter the expected alcohol by volume.")
    @DecimalMin(value = "0.1", message = "ABV must be greater than zero.")
    @DecimalMax(value = "100", message = "ABV cannot exceed 100%.")
    @Digits(integer = 3, fraction = 2, message = "Use no more than two decimal places.")
    private BigDecimal abv;

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public BigDecimal getAbv() {
        return abv;
    }

    public void setAbv(BigDecimal abv) {
        this.abv = abv;
    }
}
