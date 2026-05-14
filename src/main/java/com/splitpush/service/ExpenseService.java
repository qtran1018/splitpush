package com.splitpush.service;

import com.splitpush.dto.BalanceDTO;
import com.splitpush.dto.ExpenseDTO;
import com.splitpush.dto.GroupBreakdownDTO;
import com.splitpush.model.Expense;
import com.splitpush.model.ExpenseParticipant;
import com.splitpush.model.TripGroup;
import com.splitpush.model.User;
import com.splitpush.repository.ExpenseParticipantRepository;
import com.splitpush.repository.ExpenseRepository;
import com.splitpush.repository.SettlementRepository;
import com.splitpush.repository.TripGroupRepository;
import com.splitpush.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Transactional
@SuppressWarnings("null")
public class ExpenseService {
    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseParticipantRepository expenseParticipantRepository;

    @Autowired
    private TripGroupRepository tripGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    public Expense createExpense(ExpenseDTO expenseDTO) {
        return createExpense(expenseDTO, null);
    }

    @CacheEvict(value = {"expenses", "expensesByGroup", "expenseById", "balances"}, allEntries = true)
    public Expense createExpense(ExpenseDTO expenseDTO, Long currentUserId) {
        TripGroup tripGroup = tripGroupRepository.findById(expenseDTO.getTripGroupId())
                .orElseThrow(() -> new RuntimeException("Trip group not found"));

        // Check if current user is a member of the group - compare by ID
        if (currentUserId != null) {
            boolean isMember = tripGroup.getMembers().stream()
                    .anyMatch(member -> member.getId().equals(currentUserId));
            if (!isMember) {
                throw new RuntimeException("You must be a member of this group to create expenses");
            }
        }

        User paidBy = userRepository.findById(expenseDTO.getPaidByUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate that paidBy is a member of the trip group - compare by ID
        boolean paidByIsMember = tripGroup.getMembers().stream()
                .anyMatch(member -> member.getId().equals(paidBy.getId()));
        if (!paidByIsMember) {
            throw new RuntimeException("User is not a member of this trip group");
        }

        Expense expense = new Expense();
        expense.setDescription(expenseDTO.getDescription());
        expense.setAmount(expenseDTO.getAmount());
        expense.setPaidBy(paidBy);
        expense.setTripGroup(tripGroup);

        expense = expenseRepository.save(expense);

        // Batch-fetch all participant users in one query
        Map<Long, User> participantMap = new HashMap<>();
        userRepository.findAllById(expenseDTO.getParticipantAmounts().keySet())
                .forEach(u -> participantMap.put(u.getId(), u));

        Set<Long> memberIds = tripGroup.getMembers().stream()
                .map(User::getId).collect(java.util.stream.Collectors.toSet());

        // Create participants
        BigDecimal totalParticipantAmount = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> entry : expenseDTO.getParticipantAmounts().entrySet()) {
            User participant = participantMap.get(entry.getKey());
            if (participant == null) throw new RuntimeException("User not found: " + entry.getKey());

            if (!memberIds.contains(participant.getId())) {
                throw new RuntimeException("User " + participant.getUsername() + " is not a member of this trip group");
            }

            ExpenseParticipant expenseParticipant = new ExpenseParticipant();
            expenseParticipant.setExpense(expense);
            expenseParticipant.setUser(participant);
            expenseParticipant.setAmount(entry.getValue());
            expenseParticipant.setIsPaid(false);

            expenseParticipantRepository.save(expenseParticipant);
            totalParticipantAmount = totalParticipantAmount.add(entry.getValue());
        }

        // Validate that participant amounts sum to expense amount
        if (totalParticipantAmount.compareTo(expenseDTO.getAmount()) != 0) {
            throw new RuntimeException("Participant amounts must sum to expense amount");
        }

        return expense;
    }

    public Page<Expense> getExpensesByTripGroup(String tripGroupId) {
        return getExpensesByTripGroup(tripGroupId, null, null, null);
    }

    @CacheEvict(value = {"expenses", "expensesByGroup", "expenseById", "balances"}, allEntries = true)
    public Expense updateExpense(Long expenseId, ExpenseDTO expenseDTO, Long currentUserId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        TripGroup tripGroup = tripGroupRepository.findById(expenseDTO.getTripGroupId())
                .orElseThrow(() -> new RuntimeException("Trip group not found"));

        // Check if current user is a member of the group - compare by ID
        if (currentUserId != null) {
            boolean isMember = tripGroup.getMembers().stream()
                    .anyMatch(member -> member.getId().equals(currentUserId));
            if (!isMember) {
                throw new RuntimeException("You must be a member of this group to edit expenses");
            }
        }

        // Update expense details
        expense.setDescription(expenseDTO.getDescription());
        expense.setAmount(expenseDTO.getAmount());

        User paidBy = userRepository.findById(expenseDTO.getPaidByUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate paidBy is a member - compare by ID
        boolean paidByIsMember = tripGroup.getMembers().stream()
                .anyMatch(member -> member.getId().equals(paidBy.getId()));
        if (!paidByIsMember) {
            throw new RuntimeException("User is not a member of this trip group");
        }
        expense.setPaidBy(paidBy);

        // Remove old participants
        expenseParticipantRepository.deleteAll(expense.getParticipants());
        expense.getParticipants().clear();

        // Batch-fetch all participant users in one query
        Map<Long, User> participantMap = new HashMap<>();
        userRepository.findAllById(expenseDTO.getParticipantAmounts().keySet())
                .forEach(u -> participantMap.put(u.getId(), u));

        Set<Long> memberIds = tripGroup.getMembers().stream()
                .map(User::getId).collect(java.util.stream.Collectors.toSet());

        // Create new participants
        BigDecimal totalParticipantAmount = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> entry : expenseDTO.getParticipantAmounts().entrySet()) {
            User participant = participantMap.get(entry.getKey());
            if (participant == null) throw new RuntimeException("User not found: " + entry.getKey());

            if (!memberIds.contains(participant.getId())) {
                throw new RuntimeException("User " + participant.getUsername() + " is not a member of this trip group");
            }

            ExpenseParticipant expenseParticipant = new ExpenseParticipant();
            expenseParticipant.setExpense(expense);
            expenseParticipant.setUser(participant);
            expenseParticipant.setAmount(entry.getValue());
            expenseParticipant.setIsPaid(false);

            expenseParticipantRepository.save(expenseParticipant);
            totalParticipantAmount = totalParticipantAmount.add(entry.getValue());
        }

        // Validate that participant amounts sum to expense amount
        if (totalParticipantAmount.compareTo(expenseDTO.getAmount()) != 0) {
            throw new RuntimeException("Participant amounts must sum to expense amount");
        }

        return expenseRepository.save(expense);
    }

    public Page<Expense> getExpensesByTripGroup(String tripGroupId, Long currentUserId) {
        return getExpensesByTripGroup(tripGroupId, currentUserId, null, null);
    }

    @Cacheable(value = "expensesByGroup", key = "#tripGroupId + ':' + (#currentUserId != null ? #currentUserId : 'anonymous') + ':' + (#page != null ? #page : 0) + ':' + (#size != null ? #size : 10)")
    public Page<Expense> getExpensesByTripGroup(String tripGroupId, Long currentUserId, Integer page, Integer size) {
        TripGroup tripGroup = tripGroupRepository.findById(tripGroupId)
                .orElseThrow(() -> new RuntimeException("Trip group not found"));

        // Check if current user is a member of the group - compare by ID
        if (currentUserId != null) {
            boolean isMember = tripGroup.getMembers().stream()
                    .anyMatch(member -> member.getId().equals(currentUserId));
            if (!isMember) {
                throw new RuntimeException("You must be a member of this group to view expenses");
            }
        }

        // Default pagination: page 0, size 10
        int pageNumber = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0) ? size : 10;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        return expenseRepository.findByTripGroupOrderByCreatedAtDesc(tripGroup, pageable);
    }

    @Cacheable(value = "expenseById", key = "#id")
    public Optional<Expense> getExpenseById(Long id) {
        return expenseRepository.findById(id);
    }

    @Cacheable(value = "balances", key = "#userId")
    public List<BalanceDTO> calculateBalances(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get all expenses where user is involved (either paid or is a participant)
        List<ExpenseParticipant> userParticipants = expenseParticipantRepository.findByUser(user);
        List<Expense> expensesPaid = new ArrayList<>(user.getExpensesPaid());

        // Calculate net balance with each user and group breakdown
        Map<Long, BigDecimal> balances = new HashMap<>();
        Map<Long, Map<String, BigDecimal>> breakdownsByName = new HashMap<>();
        Map<Long, Map<String, GroupBreakdownDTO>> breakdownsById = new HashMap<>();

        // Helper to add to balance and breakdown
        java.util.function.BiConsumer<Long, Adjustment> applyAdjustment = (otherUserId, adjustment) -> {
            // net balance
            balances.put(otherUserId,
                    balances.getOrDefault(otherUserId, BigDecimal.ZERO).add(adjustment.amount()));

            // per-group breakdown (by name for legacy display)
            Map<String, BigDecimal> perGroupName = breakdownsByName.computeIfAbsent(otherUserId, k -> new HashMap<>());
            perGroupName.put(adjustment.groupName(),
                    perGroupName.getOrDefault(adjustment.groupName(), BigDecimal.ZERO).add(adjustment.amount()));

            // per-group breakdown with stable group IDs (prevents collisions when names repeat)
            Map<String, GroupBreakdownDTO> perGroupId = breakdownsById.computeIfAbsent(otherUserId, k -> new HashMap<>());
            GroupBreakdownDTO current = perGroupId.getOrDefault(
                    adjustment.groupId(),
                    new GroupBreakdownDTO(adjustment.groupId(), adjustment.groupName(), BigDecimal.ZERO)
            );
            current.setAmount(current.getAmount().add(adjustment.amount()));
            perGroupId.put(adjustment.groupId(), current);
        };

        // For expenses user paid: add to balances (others owe user)
        for (Expense expense : expensesPaid) {
            String groupId = expense.getTripGroup().getId();
            String groupName = expense.getTripGroup().getName();
            for (ExpenseParticipant participant : expense.getParticipants()) {
                if (!participant.getUser().getId().equals(userId) && !participant.getIsPaid()) {
                    Long otherUserId = participant.getUser().getId();
                    applyAdjustment.accept(otherUserId, new Adjustment(participant.getAmount(), groupId, groupName));
                }
            }
        }

        // For expenses user participated in: subtract from balances (user owes others)
        for (ExpenseParticipant participant : userParticipants) {
            if (!participant.getIsPaid()) {
                Long paidByUserId = participant.getExpense().getPaidBy().getId();
                if (!paidByUserId.equals(userId)) {
                    String groupId = participant.getExpense().getTripGroup().getId();
                    String groupName = participant.getExpense().getTripGroup().getName();
                    applyAdjustment.accept(paidByUserId, new Adjustment(participant.getAmount().negate(), groupId, groupName));
                }
            }
        }

        // Apply settlements: settlements reduce the balance
        // If user paid someone (payer = user), that reduces what user owes them
        //   - If user owes them $100 and pays $100, balance goes from -$100 to $0 (add +$100)
        // If user received payment (payee = user), that reduces what others owe user
        //   - If they owe user $100 and pay $100, balance goes from +$100 to $0 (subtract $100, i.e., add -$100)
        List<com.splitpush.model.Settlement> settlements = settlementRepository.findByPayer(user);
        settlements.addAll(settlementRepository.findByPayee(user));
        
        for (com.splitpush.model.Settlement settlement : settlements) {
            Long payerId = settlement.getPayer().getId();
            Long payeeId = settlement.getPayee().getId();
            String groupId = settlement.getTripGroup().getId();
            String groupName = settlement.getTripGroup().getName();
            BigDecimal amount = settlement.getAmount();
            
            if (payerId.equals(userId)) {
                // User paid someone - this REDUCES what user owes them
                // Balance increases (becomes less negative or more positive)
                // Example: user owes $100, pays $100 -> balance goes from -$100 to $0 (add +$100)
                applyAdjustment.accept(payeeId, new Adjustment(amount, groupId, groupName));
            } else if (payeeId.equals(userId)) {
                // User received payment - this REDUCES what others owe user
                // Balance decreases (becomes less positive or more negative)
                // Example: they owe user $100, pay $100 -> balance goes from +$100 to $0 (add -$100)
                applyAdjustment.accept(payerId, new Adjustment(amount.negate(), groupId, groupName));
            }
        }

        // Batch-fetch all users with non-zero balances in one query
        List<Long> nonZeroUserIds = balances.entrySet().stream()
                .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) != 0)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
        Map<Long, User> userMap = new HashMap<>();
        userRepository.findAllById(nonZeroUserIds).forEach(u -> userMap.put(u.getId(), u));

        // Convert to BalanceDTO list
        List<BalanceDTO> balanceList = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : balances.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) != 0) {
                User otherUser = userMap.get(entry.getKey());
                if (otherUser == null) continue;

                Map<String, BigDecimal> perGroup = breakdownsByName.getOrDefault(entry.getKey(), Collections.emptyMap());
                // Normalize scales (by name)
                Map<String, BigDecimal> scaledPerGroup = new HashMap<>();
                for (Map.Entry<String, BigDecimal> gEntry : perGroup.entrySet()) {
                    scaledPerGroup.put(gEntry.getKey(), gEntry.getValue().setScale(2, RoundingMode.HALF_UP));
                }

                // Normalize scales (by id) and convert to list
                Map<String, GroupBreakdownDTO> perGroupById = breakdownsById.getOrDefault(entry.getKey(), Collections.emptyMap());
                java.util.List<GroupBreakdownDTO> scaledPerGroupById = new ArrayList<>();
                for (GroupBreakdownDTO dto : perGroupById.values()) {
                    GroupBreakdownDTO clone = new GroupBreakdownDTO(dto.getGroupId(), dto.getGroupName(), dto.getAmount().setScale(2, RoundingMode.HALF_UP));
                    scaledPerGroupById.add(clone);
                }

                BalanceDTO balanceDTO = new BalanceDTO(
                        otherUser.getId(),
                        otherUser.getUsername(),
                        otherUser.getName(),
                        entry.getValue().setScale(2, RoundingMode.HALF_UP),
                        scaledPerGroup
                );
                balanceDTO.setGroupBreakdownDetails(scaledPerGroupById);
                balanceList.add(balanceDTO);
            }
        }

        // Sort by balance (highest owed to user first, then highest user owes)
        balanceList.sort((a, b) -> b.getNetBalance().compareTo(a.getNetBalance()));

        return balanceList;
    }

    private record Adjustment(BigDecimal amount, String groupId, String groupName) {}

    /**
     * Calculate the balance owed by payerUserId to payeeUserId in a specific group.
     * Returns negative value if payer owes payee, positive if payee owes payer, zero if balanced.
     */
    public BigDecimal calculateBalanceForGroup(Long payerUserId, Long payeeUserId, String groupId) {
        TripGroup tripGroup = tripGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Trip group not found"));
        
        // Validate users exist (but don't need to store them)
        userRepository.findById(payerUserId)
                .orElseThrow(() -> new RuntimeException("Payer user not found"));
        userRepository.findById(payeeUserId)
                .orElseThrow(() -> new RuntimeException("Payee user not found"));
        
        BigDecimal balance = BigDecimal.ZERO;
        
        // Fetch expenses with participants in one query to avoid lazy-load N+1
        List<Expense> groupExpenses = expenseRepository.findByTripGroupWithParticipants(tripGroup);
        
        // Calculate from expenses
        for (Expense expense : groupExpenses) {
            Long paidByUserId = expense.getPaidBy().getId();
            
            // If payee paid, and payer participated, payer owes payee
            if (paidByUserId.equals(payeeUserId)) {
                for (ExpenseParticipant participant : expense.getParticipants()) {
                    if (participant.getUser().getId().equals(payerUserId) && !participant.getIsPaid()) {
                        balance = balance.subtract(participant.getAmount()); // Negative = payer owes
                    }
                }
            }
            
            // If payer paid, and payee participated, payee owes payer (so payer doesn't owe payee)
            if (paidByUserId.equals(payerUserId)) {
                for (ExpenseParticipant participant : expense.getParticipants()) {
                    if (participant.getUser().getId().equals(payeeUserId) && !participant.getIsPaid()) {
                        balance = balance.add(participant.getAmount()); // Positive = payee owes payer
                    }
                }
            }
        }
        
        // Apply existing settlements for this group
        List<com.splitpush.model.Settlement> settlements = settlementRepository.findByTripGroup(tripGroup);
        for (com.splitpush.model.Settlement settlement : settlements) {
            Long settlementPayerId = settlement.getPayer().getId();
            Long settlementPayeeId = settlement.getPayee().getId();
            BigDecimal settlementAmount = settlement.getAmount();
            
            // If payer paid payee in settlement, reduce what payer owes
            if (settlementPayerId.equals(payerUserId) && settlementPayeeId.equals(payeeUserId)) {
                balance = balance.add(settlementAmount); // Reduces debt (less negative)
            }
            
            // If payee paid payer in settlement, increase what payer owes (reverse settlement)
            if (settlementPayerId.equals(payeeUserId) && settlementPayeeId.equals(payerUserId)) {
                balance = balance.subtract(settlementAmount); // Increases debt (more negative)
            }
        }
        
        return balance.setScale(2, RoundingMode.HALF_UP);
    }

    @CacheEvict(value = {"expenses", "expensesByGroup", "expenseById", "balances"}, allEntries = true)
    public void markExpenseParticipantAsPaid(Long expenseId, Long userId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        ExpenseParticipant participant = expense.getParticipants().stream()
                .filter(p -> p.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        participant.setIsPaid(true);
        expenseParticipantRepository.save(participant);
    }

    @CacheEvict(value = {"expenses", "expensesByGroup", "expenseById", "balances"}, allEntries = true)
    public void deleteExpense(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        // Delete all participants first
        expenseParticipantRepository.deleteAll(expense.getParticipants());
        
        // Then delete the expense
        expenseRepository.delete(expense);
    }
}

