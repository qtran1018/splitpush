package com.splitpush.service;

import com.splitpush.dto.SettlementDTO;
import com.splitpush.model.Settlement;
import com.splitpush.model.TripGroup;
import com.splitpush.model.User;
import com.splitpush.repository.SettlementRepository;
import com.splitpush.repository.TripGroupRepository;
import com.splitpush.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
@SuppressWarnings("null")
public class SettlementService {
    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private TripGroupRepository tripGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseService expenseService;

    @CacheEvict(value = {"settlements", "settlementsByGroup", "settlementsByUser", "balances"}, allEntries = true)
    public Settlement createSettlement(SettlementDTO settlementDTO, Long payerUserId) {
        TripGroup tripGroup = tripGroupRepository.findById(settlementDTO.getTripGroupId())
                .orElseThrow(() -> new RuntimeException("Trip group not found"));

        User payer = userRepository.findById(payerUserId)
                .orElseThrow(() -> new RuntimeException("Payer user not found"));

        User payee = userRepository.findById(settlementDTO.getPayeeUserId())
                .orElseThrow(() -> new RuntimeException("Payee user not found"));

        // Validate both users are members of the group - compare by ID
        boolean payerIsMember = tripGroup.getMembers().stream()
                .anyMatch(member -> member.getId().equals(payer.getId()));
        boolean payeeIsMember = tripGroup.getMembers().stream()
                .anyMatch(member -> member.getId().equals(payee.getId()));
        if (!payerIsMember || !payeeIsMember) {
            throw new RuntimeException("Both users must be members of the trip group");
        }

        // Validate payer and payee are different
        if (payer.getId().equals(payee.getId())) {
            throw new RuntimeException("Payer and payee cannot be the same user");
        }

        // Validate settlement amount doesn't exceed what's owed
        BigDecimal currentBalance = expenseService.calculateBalanceForGroup(
                payer.getId(), 
                payee.getId(), 
                settlementDTO.getTripGroupId()
        );
        
        // Current balance is negative if payer owes payee (payer needs to pay)
        // If current balance is positive or zero, payer doesn't owe anything
        if (currentBalance.compareTo(BigDecimal.ZERO) >= 0) {
            throw new RuntimeException("You don't owe this user anything in this group. You can only record settlements for amounts you owe.");
        }
        
        // Check if settlement amount exceeds what's owed (currentBalance is negative, so we use absolute value)
        BigDecimal amountOwed = currentBalance.abs();
        if (settlementDTO.getAmount().compareTo(amountOwed) > 0) {
            throw new RuntimeException(
                String.format("Settlement amount (%.2f) exceeds amount owed (%.2f) for this group.", 
                    settlementDTO.getAmount().doubleValue(), 
                    amountOwed.doubleValue())
            );
        }

        Settlement settlement = new Settlement();
        settlement.setPayer(payer);
        settlement.setPayee(payee);
        settlement.setAmount(settlementDTO.getAmount());
        settlement.setTripGroup(tripGroup);

        return settlementRepository.save(settlement);
    }

    @Cacheable(value = "settlementsByGroup", key = "#tripGroupId")
    public List<Settlement> getSettlementsByGroup(String tripGroupId) {
        TripGroup tripGroup = tripGroupRepository.findById(tripGroupId)
                .orElseThrow(() -> new RuntimeException("Trip group not found"));
        return settlementRepository.findByTripGroup(tripGroup);
    }

    @Cacheable(value = "settlementsByUser", key = "#userId")
    public List<Settlement> getSettlementsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get settlements where user is either payer or payee
        List<Settlement> asPayer = settlementRepository.findByPayer(user);
        List<Settlement> asPayee = settlementRepository.findByPayee(user);
        
        // Combine and return (could use a Set to avoid duplicates if needed, but shouldn't be necessary)
        asPayer.addAll(asPayee);
        return asPayer;
    }
}

