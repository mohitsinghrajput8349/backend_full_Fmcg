package fmcg.distribution.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import fmcg.distribution.enums.Role;

@Data
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Phone is required")
    private String phone;

    private Role role = Role.SHOP_OWNER;
    private Integer creditPeriodDays = 7;
}