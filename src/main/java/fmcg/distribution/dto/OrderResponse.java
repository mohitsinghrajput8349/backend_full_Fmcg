package fmcg.distribution.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import fmcg.distribution.enums.OrderStatus;
import fmcg.distribution.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private Long id;
    private Long shopOwnerId;
    private String shopOwnerName;
    private List<OrderItemResponse> items;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private LocalDateTime deliveryDate;
    private LocalDateTime paymentDueDate;
    private Long createdBy;
    private LocalDateTime createdAt;
}