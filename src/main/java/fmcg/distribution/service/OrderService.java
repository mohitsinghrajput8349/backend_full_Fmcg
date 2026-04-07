package fmcg.distribution.service;

import fmcg.distribution.dto.*;
import fmcg.distribution.entity.*;
import fmcg.distribution.repository.*;
import fmcg.distribution.enums.*;
import fmcg.distribution.security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Transactional
    public OrderResponse createOrder(OrderRequest request, Authentication authentication) {
        String currentUserEmail = authentication.getName();
        User currentUser = userDetailsService.loadUserEntityByEmail(currentUserEmail);

        User shopOwner;
        if (currentUser.getRole() == Role.SHOP_OWNER) {
            shopOwner = currentUser;
        } else {
            // Admin creating order for a shop
            if (request.getShopOwnerId() == null) {
                throw new RuntimeException("shop_owner_id required for admin");
            }
            shopOwner = userRepository.findById(request.getShopOwnerId())
                    .orElseThrow(() -> new RuntimeException("Shop owner not found"));
        }

        Order order = new Order();
        order.setShopOwner(shopOwner);
        order.setShopOwnerName(shopOwner.getName());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedBy(currentUser);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (var itemReq : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setProductId(itemReq.getProductId());
            item.setProductName(itemReq.getProductName());
            item.setQuantity(itemReq.getQuantity());
            item.setPrice(itemReq.getPrice());
            item.setTotal(itemReq.getTotal());
            order.addItem(item);
            totalAmount = totalAmount.add(itemReq.getTotal());
        }
        order.setTotalAmount(totalAmount);

        order = orderRepository.save(order);

        // Create notification
        String paymentInfo = request.getPaymentMethod() == PaymentMethod.COD ? "Cash on Delivery" : "Online Payment";
        Notification notification = new Notification();
        notification.setUser(shopOwner);
        notification.setMessage(String.format("New order #%d created for ₹%.2f (%s)", 
                order.getId(), totalAmount, paymentInfo));
        notification.setType("order_update");
        notification.setIsRead(false);
        notificationRepository.save(notification);

        return mapToResponse(order);
    }

    public List<OrderResponse> getOrders(Authentication authentication) {
        String email = authentication.getName();
        User user = userDetailsService.loadUserEntityByEmail(email);

        List<Order> orders;
        if (user.getRole() == Role.ADMIN) {
            orders = orderRepository.findAllByOrderByCreatedAtDesc();
        } else {
            orders = orderRepository.findByShopOwnerOrderByCreatedAtDesc(user);
        }

        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(Long id, Authentication authentication) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        String email = authentication.getName();
        User user = userDetailsService.loadUserEntityByEmail(email);

        if (user.getRole() != Role.ADMIN && !order.getShopOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        return mapToResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(status);

        if (status == OrderStatus.DELIVERED && order.getDeliveryDate() == null) {
            LocalDateTime deliveryDate = LocalDateTime.now();
            order.setDeliveryDate(deliveryDate);
            
            Integer creditDays = order.getShopOwner().getCreditPeriodDays();
            order.setPaymentDueDate(deliveryDate.plusDays(creditDays));

            // Create payment reminder notification
            Notification notification = new Notification();
            notification.setUser(order.getShopOwner());
            notification.setMessage(String.format("Order #%d delivered. Payment of ₹%.2f due by %s",
                    order.getId(), order.getTotalAmount(), 
                    order.getPaymentDueDate().toLocalDate().toString()));
            notification.setType("payment_reminder");
            notification.setIsRead(false);
            notificationRepository.save(notification);
        }

        order = orderRepository.save(order);
        return mapToResponse(order);
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getTotal()
                ))
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getShopOwner().getId(),
                order.getShopOwnerName(),
                items,
                order.getTotalAmount(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getDeliveryDate(),
                order.getPaymentDueDate(),
                order.getCreatedBy().getId(),
                order.getCreatedAt()
        );
    }
}