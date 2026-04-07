package fmcg.distribution.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Email is required")
    @Email
    private String email;

    @NotBlank(message = "Reset code is required")
    private String resetCode;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
}