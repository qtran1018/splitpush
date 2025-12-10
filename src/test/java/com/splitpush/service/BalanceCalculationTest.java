package com.splitpush.service;

import com.splitpush.dto.BalanceDTO;
import com.splitpush.model.Expense;
import com.splitpush.model.ExpenseParticipant;
import com.splitpush.model.Settlement;
import com.splitpush.model.TripGroup;
import com.splitpush.model.User;
import com.splitpush.repository.ExpenseParticipantRepository;
import com.splitpush.repository.ExpenseRepository;
import com.splitpush.repository.SettlementRepository;
import com.splitpush.repository.TripGroupRepository;
import com.splitpush.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for balance calculations including:
 * - Addition and subtraction when adding expenses
 * - Complex scenarios with multiple expenses
 * - Settlements affecting balances
 * - Multiple groups
 */
@ExtendWith(MockitoExtension.class)
class BalanceCalculationTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseParticipantRepository expenseParticipantRepository;

    @Mock
    private TripGroupRepository tripGroupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @InjectMocks
    private ExpenseService expenseService;

    private User user1;
    private User user2;
    private User user3;
    private TripGroup tripGroup;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setName("User One");

        user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setName("User Two");

        user3 = new User();
        user3.setId(3L);
        user3.setUsername("user3");
        user3.setName("User Three");

        tripGroup = new TripGroup();
        tripGroup.setId("group1");
        tripGroup.setName("Test Group");
        tripGroup.setMembers(new HashSet<>(Arrays.asList(user1, user2, user3)));
    }

    @Test
    void testBalanceCalculation_UserPays_OthersOwe() {
        // Scenario: user1 pays $100, split equally among 3 users
        // Expected: user2 owes user1 $33.33, user3 owes user1 $33.34

        Expense expense = createExpense(1L, "Dinner", new BigDecimal("100.00"), user1, tripGroup);
        
        ExpenseParticipant p1 = createParticipant(expense, user1, new BigDecimal("33.33"), false);
        ExpenseParticipant p2 = createParticipant(expense, user2, new BigDecimal("33.33"), false);
        ExpenseParticipant p3 = createParticipant(expense, user3, new BigDecimal("33.34"), false);
        
        expense.setParticipants(new HashSet<>(Arrays.asList(p1, p2, p3)));

        // Set up user1's expenses paid
        Set<Expense> expensesPaid = new HashSet<>();
        expensesPaid.add(expense);
        user1.setExpensesPaid(expensesPaid);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(expenseParticipantRepository.findByUser(user1)).thenReturn(Arrays.asList(p1));
        when(settlementRepository.findByPayer(user1)).thenReturn(new ArrayList<>());
        when(settlementRepository.findByPayee(user1)).thenReturn(new ArrayList<>());
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));

        List<BalanceDTO> balances = expenseService.calculateBalances(1L);

        assertEquals(2, balances.size());
        
        // Check user2 balance
        BalanceDTO balance2 = balances.stream()
            .filter(b -> b.getUserId().equals(2L))
            .findFirst()
            .orElse(null);
        assertNotNull(balance2);
        assertTrue(balance2.getNetBalance().compareTo(new BigDecimal("33.33")) == 0 ||
                   balance2.getNetBalance().compareTo(new BigDecimal("33.34")) == 0);
        
        // Check user3 balance
        BalanceDTO balance3 = balances.stream()
            .filter(b -> b.getUserId().equals(3L))
            .findFirst()
            .orElse(null);
        assertNotNull(balance3);
        assertTrue(balance3.getNetBalance().compareTo(new BigDecimal("33.33")) == 0 ||
                   balance3.getNetBalance().compareTo(new BigDecimal("33.34")) == 0);
    }

    @Test
    void testBalanceCalculation_UserParticipates_UserOwes() {
        // Scenario: user2 pays $100, user1 participates with $50 share
        // Expected: user1 owes user2 $50 (negative balance)

        Expense expense = createExpense(1L, "Lunch", new BigDecimal("100.00"), user2, tripGroup);
        
        ExpenseParticipant p1 = createParticipant(expense, user1, new BigDecimal("50.00"), false);
        ExpenseParticipant p2 = createParticipant(expense, user2, new BigDecimal("50.00"), false);
        
        expense.setParticipants(new HashSet<>(Arrays.asList(p1, p2)));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(expenseParticipantRepository.findByUser(user1)).thenReturn(Arrays.asList(p1));
        when(settlementRepository.findByPayer(user1)).thenReturn(new ArrayList<>());
        when(settlementRepository.findByPayee(user1)).thenReturn(new ArrayList<>());
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        List<BalanceDTO> balances = expenseService.calculateBalances(1L);

        assertEquals(1, balances.size());
        BalanceDTO balance = balances.get(0);
        assertEquals(2L, balance.getUserId());
        assertEquals(new BigDecimal("-50.00"), balance.getNetBalance()); // Negative = user1 owes
    }

    @Test
    void testBalanceCalculation_MultipleExpenses_NetBalance() {
        // Scenario: 
        // - Expense 1: user1 pays $100, user2 owes $50, user3 owes $50
        // - Expense 2: user2 pays $60, user1 owes $30, user3 owes $30
        // Expected net: user2 owes user1 $20, user3 owes user1 $20

        Expense expense1 = createExpense(1L, "Dinner", new BigDecimal("100.00"), user1, tripGroup);
        ExpenseParticipant p1_1 = createParticipant(expense1, user1, new BigDecimal("50.00"), false);
        ExpenseParticipant p1_2 = createParticipant(expense1, user2, new BigDecimal("50.00"), false);
        ExpenseParticipant p1_3 = createParticipant(expense1, user3, new BigDecimal("50.00"), false);
        expense1.setParticipants(new HashSet<>(Arrays.asList(p1_1, p1_2, p1_3)));

        Expense expense2 = createExpense(2L, "Lunch", new BigDecimal("60.00"), user2, tripGroup);
        ExpenseParticipant p2_1 = createParticipant(expense2, user1, new BigDecimal("30.00"), false);
        ExpenseParticipant p2_2 = createParticipant(expense2, user2, new BigDecimal("30.00"), false);
        ExpenseParticipant p2_3 = createParticipant(expense2, user3, new BigDecimal("30.00"), false);
        expense2.setParticipants(new HashSet<>(Arrays.asList(p2_1, p2_2, p2_3)));

        // Set up user1's expenses paid - user1 paid expense1
        Set<Expense> expensesPaid = new HashSet<>();
        expensesPaid.add(expense1);
        user1.setExpensesPaid(expensesPaid);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(expenseParticipantRepository.findByUser(user1))
            .thenReturn(Arrays.asList(p1_1, p2_1));
        when(settlementRepository.findByPayer(user1)).thenReturn(new ArrayList<>());
        when(settlementRepository.findByPayee(user1)).thenReturn(new ArrayList<>());
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));

        List<BalanceDTO> balances = expenseService.calculateBalances(1L);

        assertEquals(2, balances.size());
        
        // user2: 
        // - From expense1 (user1 paid): user2 owes user1 $50 (positive for user1)
        // - From expense2 (user2 paid, user1 participated): user1 owes user2 $30 (negative for user1)
        // Net: user2 owes user1 $20
        BalanceDTO balance2 = balances.stream()
            .filter(b -> b.getUserId().equals(2L))
            .findFirst()
            .orElse(null);
        assertNotNull(balance2);
        // The balance should be $20 (user2 owes user1)
        assertEquals(new BigDecimal("20.00"), balance2.getNetBalance());
        
        // user3: 
        // - From expense1 (user1 paid): user3 owes user1 $50 (positive for user1)
        // - From expense2 (user2 paid, user1 participated): user1 owes user2 $30, but user3 also participated
        //   Actually, from user1's perspective: user3 doesn't directly interact in expense2
        //   But wait, let me recalculate: expense2 is paid by user2, user1 and user3 both participate
        //   So user1 owes user2 $30, but this doesn't affect user1's balance with user3
        // Net: user3 owes user1 $50 (only from expense1)
        BalanceDTO balance3 = balances.stream()
            .filter(b -> b.getUserId().equals(3L))
            .findFirst()
            .orElse(null);
        assertNotNull(balance3);
        assertEquals(new BigDecimal("50.00"), balance3.getNetBalance());
    }

    @Test
    void testBalanceCalculation_WithSettlement_ReducesDebt() {
        // Scenario:
        // - Expense: user2 pays $100, user1 owes $100
        // - Settlement: user1 pays user2 $50
        // Expected: user1 still owes user2 $50

        Expense expense = createExpense(1L, "Dinner", new BigDecimal("100.00"), user2, tripGroup);
        ExpenseParticipant p1 = createParticipant(expense, user1, new BigDecimal("100.00"), false);
        expense.setParticipants(new HashSet<>(Arrays.asList(p1)));

        Settlement settlement = new Settlement();
        settlement.setId(1L);
        settlement.setPayer(user1);
        settlement.setPayee(user2);
        settlement.setAmount(new BigDecimal("50.00"));
        settlement.setTripGroup(tripGroup);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(expenseParticipantRepository.findByUser(user1)).thenReturn(Arrays.asList(p1));
        when(settlementRepository.findByPayer(user1)).thenReturn(Arrays.asList(settlement));
        when(settlementRepository.findByPayee(user1)).thenReturn(Collections.emptyList());
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        List<BalanceDTO> balances = expenseService.calculateBalances(1L);

        assertEquals(1, balances.size());
        BalanceDTO balance = balances.get(0);
        assertEquals(2L, balance.getUserId());
        assertEquals(new BigDecimal("-50.00"), balance.getNetBalance()); // Still owes $50
    }

    @Test
    void testBalanceCalculation_SettlementFullyPaysDebt() {
        // Scenario:
        // - Expense: user2 pays $100, user1 owes $100
        // - Settlement: user1 pays user2 $100
        // Expected: balance should be $0 (no balance entry)

        Expense expense = createExpense(1L, "Dinner", new BigDecimal("100.00"), user2, tripGroup);
        ExpenseParticipant p1 = createParticipant(expense, user1, new BigDecimal("100.00"), false);
        expense.setParticipants(new HashSet<>(Arrays.asList(p1)));

        Settlement settlement = new Settlement();
        settlement.setPayer(user1);
        settlement.setPayee(user2);
        settlement.setAmount(new BigDecimal("100.00"));
        settlement.setTripGroup(tripGroup);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(expenseParticipantRepository.findByUser(user1)).thenReturn(Arrays.asList(p1));
        when(settlementRepository.findByPayer(user1)).thenReturn(Arrays.asList(settlement));
        when(settlementRepository.findByPayee(user1)).thenReturn(new ArrayList<>());
        // Note: userRepository.findById(2L) is not needed since balance is zero and user2 won't appear in results

        List<BalanceDTO> balances = expenseService.calculateBalances(1L);

        // Balance should be zero, so no entries should be returned (balances with zero are filtered out)
        assertTrue(balances.isEmpty() || balances.stream()
            .allMatch(b -> b.getNetBalance().compareTo(BigDecimal.ZERO) == 0));
    }

    @Test
    void testBalanceCalculation_ReverseSettlement() {
        // Scenario:
        // - Expense: user1 pays $100, user2 owes $100
        // - Settlement: user2 pays user1 $30 (reverse - payee pays payer)
        // Expected: user2 still owes user1 $70

        Expense expense = createExpense(1L, "Dinner", new BigDecimal("100.00"), user1, tripGroup);
        ExpenseParticipant p2 = createParticipant(expense, user2, new BigDecimal("100.00"), false);
        expense.setParticipants(new HashSet<>(Arrays.asList(p2)));

        Settlement settlement = new Settlement();
        settlement.setPayer(user2);
        settlement.setPayee(user1);
        settlement.setAmount(new BigDecimal("30.00"));
        settlement.setTripGroup(tripGroup);

        // Set up user1's expenses paid
        Set<Expense> expensesPaid = new HashSet<>();
        expensesPaid.add(expense);
        user1.setExpensesPaid(expensesPaid);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(expenseParticipantRepository.findByUser(user1)).thenReturn(new ArrayList<>());
        when(settlementRepository.findByPayer(user1)).thenReturn(new ArrayList<>());
        when(settlementRepository.findByPayee(user1)).thenReturn(Arrays.asList(settlement));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        List<BalanceDTO> balances = expenseService.calculateBalances(1L);

        assertEquals(1, balances.size());
        BalanceDTO balance = balances.get(0);
        assertEquals(2L, balance.getUserId());
        assertEquals(new BigDecimal("70.00"), balance.getNetBalance()); // user2 owes $70
    }

    @Test
    void testBalanceCalculation_GroupBreakdown() {
        // Scenario: Multiple groups with different balances
        TripGroup group1 = new TripGroup();
        group1.setId("group1");
        group1.setName("Group 1");
        group1.setMembers(new HashSet<>(Arrays.asList(user1, user2)));

        TripGroup group2 = new TripGroup();
        group2.setId("group2");
        group2.setName("Group 2");
        group2.setMembers(new HashSet<>(Arrays.asList(user1, user2)));

        Expense expense1 = createExpense(1L, "Dinner", new BigDecimal("60.00"), user2, group1);
        ExpenseParticipant p1 = createParticipant(expense1, user1, new BigDecimal("60.00"), false);
        expense1.setParticipants(new HashSet<>(Arrays.asList(p1)));

        Expense expense2 = createExpense(2L, "Lunch", new BigDecimal("40.00"), user2, group2);
        ExpenseParticipant p2 = createParticipant(expense2, user1, new BigDecimal("40.00"), false);
        expense2.setParticipants(new HashSet<>(Arrays.asList(p2)));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(expenseParticipantRepository.findByUser(user1))
            .thenReturn(Arrays.asList(p1, p2));
        when(settlementRepository.findByPayer(user1)).thenReturn(new ArrayList<>());
        when(settlementRepository.findByPayee(user1)).thenReturn(new ArrayList<>());
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        List<BalanceDTO> balances = expenseService.calculateBalances(1L);

        assertEquals(1, balances.size());
        BalanceDTO balance = balances.get(0);
        assertEquals(2L, balance.getUserId());
        assertEquals(new BigDecimal("-100.00"), balance.getNetBalance());
        
        // Check group breakdown
        assertNotNull(balance.getGroupBreakdown());
        assertEquals(2, balance.getGroupBreakdown().size());
        assertEquals(new BigDecimal("-60.00"), balance.getGroupBreakdown().get("Group 1"));
        assertEquals(new BigDecimal("-40.00"), balance.getGroupBreakdown().get("Group 2"));
    }

    // Helper methods
    private Expense createExpense(Long id, String description, BigDecimal amount, User paidBy, TripGroup group) {
        Expense expense = new Expense();
        expense.setId(id);
        expense.setDescription(description);
        expense.setAmount(amount);
        expense.setPaidBy(paidBy);
        expense.setTripGroup(group);
        return expense;
    }

    private ExpenseParticipant createParticipant(Expense expense, User user, BigDecimal amount, Boolean isPaid) {
        ExpenseParticipant participant = new ExpenseParticipant();
        participant.setExpense(expense);
        participant.setUser(user);
        participant.setAmount(amount);
        participant.setIsPaid(isPaid);
        return participant;
    }
}

