package fmcg.distribution.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}