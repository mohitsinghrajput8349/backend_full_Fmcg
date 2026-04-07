package fmcg.distribution.service;

import fmcg.distribution.entity.User;
import fmcg.distribution.repository.UserRepository;
import fmcg.distribution.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShopOwnerService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getAllShopOwners() {
        return userRepository.findByRoleAndIsActiveTrue(Role.SHOP_OWNER);
    }

    @Transactional
    public void updateCreditPeriod(Long shopId, Integer creditPeriodDays) {
        userRepository.findById(shopId).ifPresent(user -> {
            user.setCreditPeriodDays(creditPeriodDays);
            userRepository.save(user);
        });
    }
}