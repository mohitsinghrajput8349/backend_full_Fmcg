package fmcg.distribution.controller;

import fmcg.distribution.entity.Order;
import fmcg.distribution.entity.Payment;
import fmcg.distribution.repository.OrderRepository;
import fmcg.distribution.repository.PaymentRepository;
import fmcg.distribution.enums.PaymentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @GetMapping("/sales")
    public ResponseEntity<?> getSalesAnalytics() {
        List<Order> orders = orderRepository.findAll();
        List<Payment> payments = paymentRepository.findAll();

        BigDecimal totalSales = orders.stream()
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal paidAmount = payments.stream()
            .filter(p -> p.getStatus() == PaymentStatus.PAID)
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingAmount = totalSales.subtract(paidAmount);

        long pendingOrders = orders.stream()
            .filter(o -> o.getStatus().name().equals("PENDING"))
            .count();

        long deliveredOrders = orders.stream()
            .filter(o -> o.getStatus().name().equals("DELIVERED"))
            .count();

        // Top products
        Map<Long, Map<String, Object>> productSales = new HashMap<>();
        for (Order order : orders) {
            for (var item : order.getItems()) {
                productSales.putIfAbsent(item.getProductId(), new HashMap<>());
                Map<String, Object> data = productSales.get(item.getProductId());
                data.put("product_name", item.getProductName());
                data.put("quantity", (Integer) data.getOrDefault("quantity", 0) + item.getQuantity());
                BigDecimal revenue = (BigDecimal) data.getOrDefault("revenue", BigDecimal.ZERO);
                data.put("revenue", revenue.add(item.getTotal()));
            }
        }

        List<Map<String, Object>> topProducts = productSales.values().stream()
            .sorted((a, b) -> ((BigDecimal) b.get("revenue")).compareTo((BigDecimal) a.get("revenue")))
            .limit(10)
            .collect(Collectors.toList());

        // Top shops
        Map<Long, Map<String, Object>> shopSales = new HashMap<>();
        for (Order order : orders) {
            shopSales.putIfAbsent(order.getShopOwner().getId(), new HashMap<>());
            Map<String, Object> data = shopSales.get(order.getShopOwner().getId());
            data.put("shop_name", order.getShopOwnerName());
            data.put("total_orders", (Integer) data.getOrDefault("total_orders", 0) + 1);
            BigDecimal amount = (BigDecimal) data.getOrDefault("total_amount", BigDecimal.ZERO);
            data.put("total_amount", amount.add(order.getTotalAmount()));
        }

        List<Map<String, Object>> topShops = shopSales.values().stream()
            .sorted((a, b) -> ((BigDecimal) b.get("total_amount")).compareTo((BigDecimal) a.get("total_amount")))
            .limit(10)
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("total_sales", totalSales);
        response.put("total_orders", orders.size());
        response.put("pending_orders", pendingOrders);
        response.put("delivered_orders", deliveredOrders);
        response.put("paid_amount", paidAmount);
        response.put("pending_amount", pendingAmount);
        response.put("top_products", topProducts);
        response.put("top_shops", topShops);

        return ResponseEntity.ok(response);
    }
}
