package fmcg.distribution.controller;

import fmcg.distribution.dto.UserResponse;
import fmcg.distribution.entity.User;
import fmcg.distribution.service.ShopOwnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/shop-owners")
public class ShopOwnerController {

    @Autowired
    private ShopOwnerService shopOwnerService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllShopOwners() {
        List<User> shops = shopOwnerService.getAllShopOwners();
        List<UserResponse> response = shops.stream()
            .map(user -> new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getRole(),
                user.getCreditPeriodDays(),
                user.getIsActive(),
                user.getCreatedAt()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/credit-period")
    public ResponseEntity<?> updateCreditPeriod(
        @PathVariable Long id,
        @RequestParam Integer creditPeriodDays
    ) {
        shopOwnerService.updateCreditPeriod(id, creditPeriodDays);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Credit period updated successfully");
        return ResponseEntity.ok(response);
    }
}