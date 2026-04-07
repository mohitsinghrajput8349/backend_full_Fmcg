package fmcg.distribution.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import fmcg.distribution.enums.PaymentMethod;
import java.util.List;

@Data
public class OrderRequest {
    private Long shopOwnerId; // Optional for shop owners
    
    @NotEmpty(message = "Order items cannot be empty")
    private List<OrderItemRequest> items;
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod = PaymentMethod.ONLINE;
}