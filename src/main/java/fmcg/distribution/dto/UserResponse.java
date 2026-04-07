package fmcg.distribution.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import fmcg.distribution.enums.Role;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private Role role;
    private Integer creditPeriodDays;
    private Boolean isActive;
    private LocalDateTime createdAt;
}