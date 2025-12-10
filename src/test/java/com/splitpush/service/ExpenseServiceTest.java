package com.splitpush.service;

import com.splitpush.dto.ExpenseDTO;
import com.splitpush.model.Expense;
import com.splitpush.model.ExpenseParticipant;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ExpenseServiceTest {

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
    private ExpenseDTO expenseDTO;

    @BeforeEach
    void setUp() {
        // Create test users
        user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setName("User One");
        user1.setEmail("user1@test.com");
        user1.setPassword("password");

        user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setName("User Two");
        user2.setEmail("user2@test.com");
        user2.setPassword("password");

        user3 = new User();
        user3.setId(3L);
        user3.setUsername("user3");
        user3.setName("User Three");
        user3.setEmail("user3@test.com");
        user3.setPassword("password");

        // Create test trip group
        tripGroup = new TripGroup();
        tripGroup.setId("group1");
        tripGroup.setName("Test Group");
        tripGroup.setDescription("Test Description");
        tripGroup.setCreatedBy(user1);
        tripGroup.setMembers(new HashSet<>(Arrays.asList(user1, user2, user3)));

        // Create expense DTO
        expenseDTO = new ExpenseDTO();
        expenseDTO.setDescription("Test Expense");
        expenseDTO.setAmount(new BigDecimal("100.00"));
        expenseDTO.setTripGroupId("group1");
        expenseDTO.setPaidByUserId(1L);
    }

    @Test
    void testCreateExpense_EqualSplit() {
        // Arrange
        Map<Long, BigDecimal> participantAmounts = new HashMap<>();
        participantAmounts.put(1L, new BigDecimal("33.33"));
        participantAmounts.put(2L, new BigDecimal("33.33"));
        participantAmounts.put(3L, new BigDecimal("33.34"));
        expenseDTO.setParticipantAmounts(participantAmounts);

        Expense savedExpense = new Expense();
        savedExpense.setId(1L);
        savedExpense.setDescription(expenseDTO.getDescription());
        savedExpense.setAmount(expenseDTO.getAmount());
        savedExpense.setPaidBy(user1);
        savedExpense.setTripGroup(tripGroup);

        when(tripGroupRepository.findById("group1")).thenReturn(Optional.of(tripGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);
        when(expenseParticipantRepository.save(any(ExpenseParticipant.class))).thenAnswer(invocation -> {
            ExpenseParticipant ep = invocation.getArgument(0);
            ep.setId(1L);
            return ep;
        });

        // Act
        Expense result = expenseService.createExpense(expenseDTO, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("Test Expense", result.getDescription());
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals(user1.getId(), result.getPaidBy().getId());
        
        // Verify participants were created
        verify(expenseParticipantRepository, times(3)).save(any(ExpenseParticipant.class));
        verify(expenseRepository, times(1)).save(any(Expense.class));
    }

    @Test
    void testCreateExpense_ParticipantAmountsMustSumToExpenseAmount() {
        // Arrange
        Map<Long, BigDecimal> participantAmounts = new HashMap<>();
        participantAmounts.put(1L, new BigDecimal("50.00"));
        participantAmounts.put(2L, new BigDecimal("30.00")); // Sum = 80, but expense is 100
        expenseDTO.setParticipantAmounts(participantAmounts);

        when(tripGroupRepository.findById("group1")).thenReturn(Optional.of(tripGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(expenseRepository.save(any(Expense.class))).thenReturn(new Expense());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            expenseService.createExpense(expenseDTO, 1L);
        });
        
        assertTrue(exception.getMessage().contains("Participant amounts must sum to expense amount"));
    }

    @Test
    void testCalculateBalances_UserPaysExpense() {
        // Arrange
        Expense expense = new Expense();
        expense.setId(1L);
        expense.setDescription("Dinner");
        expense.setAmount(new BigDecimal("100.00"));
        expense.setPaidBy(user1);
        expense.setTripGroup(tripGroup);

        ExpenseParticipant participant1 = new ExpenseParticipant();
        participant1.setUser(user1);
        participant1.setAmount(new BigDecimal("33.33"));
        participant1.setIsPaid(false);
        participant1.setExpense(expense);

        ExpenseParticipant participant2 = new ExpenseParticipant();
        participant2.setUser(user2);
        participant2.setAmount(new BigDecimal("33.33"));
        participant2.setIsPaid(false);
        participant2.setExpense(expense);

        ExpenseParticipant participant3 = new ExpenseParticipant();
        participant3.setUser(user3);
        participant3.setAmount(new BigDecimal("33.34"));
        participant3.setIsPaid(false);
        participant3.setExpense(expense);

        expense.setParticipants(new HashSet<>(Arrays.asList(participant1, participant2, participant3)));

        // Mock user1's expenses paid
        Set<Expense> expensesPaid = new HashSet<>();
        expensesPaid.add(expense);
        user1.setExpensesPaid(expensesPaid);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(expenseParticipantRepository.findByUser(user1)).thenReturn(Arrays.asList(participant1));
        when(settlementRepository.findByPayer(user1)).thenReturn(new ArrayList<>());
        when(settlementRepository.findByPayee(user1)).thenReturn(new ArrayList<>());
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));

        // Act
        List<com.splitpush.dto.BalanceDTO> balances = expenseService.calculateBalances(1L);

        // Assert
        assertNotNull(balances);
        assertEquals(2, balances.size()); // user1 should have balances with user2 and user3
        
        // user1 paid, so user2 and user3 owe user1
        // user2 owes: 33.33, user3 owes: 33.34
        boolean foundUser2 = false;
        boolean foundUser3 = false;
        for (com.splitpush.dto.BalanceDTO balance : balances) {
            if (balance.getUserId().equals(2L)) {
                assertEquals(new BigDecimal("33.33"), balance.getNetBalance());
                foundUser2 = true;
            } else if (balance.getUserId().equals(3L)) {
                assertEquals(new BigDecimal("33.34"), balance.getNetBalance());
                foundUser3 = true;
            }
        }
        assertTrue(foundUser2, "Balance with user2 should exist");
        assertTrue(foundUser3, "Balance with user3 should exist");
    }

    @Test
    void testCalculateBalances_UserParticipatesInExpense() {
        // Arrange
        Expense expense = new Expense();
        expense.setId(1L);
        expense.setDescription("Dinner");
        expense.setAmount(new BigDecimal("100.00"));
        expense.setPaidBy(user2); // user2 paid
        expense.setTripGroup(tripGroup);

        ExpenseParticipant participant1 = new ExpenseParticipant();
        participant1.setUser(user1);
        participant1.setAmount(new BigDecimal("50.00"));
        participant1.setIsPaid(false);
        participant1.setExpense(expense);

        ExpenseParticipant participant2 = new ExpenseParticipant();
        participant2.setUser(user2);
        participant2.setAmount(new BigDecimal("50.00"));
        participant2.setIsPaid(false);
        participant2.setExpense(expense);

        expense.setParticipants(new HashSet<>(Arrays.asList(participant1, participant2)));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(expenseParticipantRepository.findByUser(user1)).thenReturn(Arrays.asList(participant1));
        when(settlementRepository.findByPayer(user1)).thenReturn(new ArrayList<>());
        when(settlementRepository.findByPayee(user1)).thenReturn(new ArrayList<>());
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        // Act
        List<com.splitpush.dto.BalanceDTO> balances = expenseService.calculateBalances(1L);

        // Assert
        assertNotNull(balances);
        assertEquals(1, balances.size());
        
        // user1 participated in expense paid by user2, so user1 owes user2
        com.splitpush.dto.BalanceDTO balance = balances.get(0);
        assertEquals(2L, balance.getUserId());
        assertEquals(new BigDecimal("-50.00"), balance.getNetBalance()); // Negative = user1 owes user2
    }

    @Test
    void testCalculateBalances_WithSettlement() {
        // Arrange
        Expense expense = new Expense();
        expense.setId(1L);
        expense.setDescription("Dinner");
        expense.setAmount(new BigDecimal("100.00"));
        expense.setPaidBy(user2); // user2 paid
        expense.setTripGroup(tripGroup);

        ExpenseParticipant participant1 = new ExpenseParticipant();
        participant1.setUser(user1);
        participant1.setAmount(new BigDecimal("100.00"));
        participant1.setIsPaid(false);
        participant1.setExpense(expense);

        expense.setParticipants(new HashSet<>(Arrays.asList(participant1)));

        // Create settlement where user1 pays user2 $50
        com.splitpush.model.Settlement settlement = new com.splitpush.model.Settlement();
        settlement.setId(1L);
        settlement.setPayer(user1);
        settlement.setPayee(user2);
        settlement.setAmount(new BigDecimal("50.00"));
        settlement.setTripGroup(tripGroup);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(expenseParticipantRepository.findByUser(user1)).thenReturn(Arrays.asList(participant1));
        when(settlementRepository.findByPayer(user1)).thenReturn(Arrays.asList(settlement));
        when(settlementRepository.findByPayee(user1)).thenReturn(Collections.emptyList());
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        // Act
        List<com.splitpush.dto.BalanceDTO> balances = expenseService.calculateBalances(1L);

        // Assert
        assertNotNull(balances);
        assertEquals(1, balances.size());
        
        // user1 originally owed user2 $100, but paid $50, so now owes $50
        com.splitpush.dto.BalanceDTO balance = balances.get(0);
        assertEquals(2L, balance.getUserId());
        assertEquals(new BigDecimal("-50.00"), balance.getNetBalance()); // Still owes $50
    }

    @Test
    void testCalculateBalanceForGroup_SimpleCase() {
        // Arrange
        Expense expense = new Expense();
        expense.setId(1L);
        expense.setDescription("Dinner");
        expense.setAmount(new BigDecimal("100.00"));
        expense.setPaidBy(user2); // user2 paid
        expense.setTripGroup(tripGroup);

        ExpenseParticipant participant1 = new ExpenseParticipant();
        participant1.setUser(user1);
        participant1.setAmount(new BigDecimal("100.00"));
        participant1.setIsPaid(false);
        participant1.setExpense(expense);

        expense.setParticipants(new HashSet<>(Arrays.asList(participant1)));

        when(tripGroupRepository.findById("group1")).thenReturn(Optional.of(tripGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(expenseRepository.findByTripGroup(tripGroup)).thenReturn(Arrays.asList(expense));
        when(settlementRepository.findByTripGroup(tripGroup)).thenReturn(new ArrayList<>());

        // Act
        BigDecimal balance = expenseService.calculateBalanceForGroup(1L, 2L, "group1");

        // Assert
        // user1 (payer) owes user2 (payee) $100, so balance should be negative
        assertEquals(new BigDecimal("-100.00"), balance);
    }

    @Test
    void testCalculateBalanceForGroup_WithSettlement() {
        // Arrange
        Expense expense = new Expense();
        expense.setId(1L);
        expense.setDescription("Dinner");
        expense.setAmount(new BigDecimal("100.00"));
        expense.setPaidBy(user2);
        expense.setTripGroup(tripGroup);

        ExpenseParticipant participant1 = new ExpenseParticipant();
        participant1.setUser(user1);
        participant1.setAmount(new BigDecimal("100.00"));
        participant1.setIsPaid(false);
        participant1.setExpense(expense);

        expense.setParticipants(new HashSet<>(Arrays.asList(participant1)));

        // Settlement: user1 pays user2 $50
        com.splitpush.model.Settlement settlement = new com.splitpush.model.Settlement();
        settlement.setPayer(user1);
        settlement.setPayee(user2);
        settlement.setAmount(new BigDecimal("50.00"));
        settlement.setTripGroup(tripGroup);

        when(tripGroupRepository.findById("group1")).thenReturn(Optional.of(tripGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(expenseRepository.findByTripGroup(tripGroup)).thenReturn(Arrays.asList(expense));
        when(settlementRepository.findByTripGroup(tripGroup)).thenReturn(Arrays.asList(settlement));

        // Act
        BigDecimal balance = expenseService.calculateBalanceForGroup(1L, 2L, "group1");

        // Assert
        // user1 originally owed $100, paid $50, so balance should be -$50
        assertEquals(new BigDecimal("-50.00"), balance);
    }

    @Test
    void testCalculateBalances_MultipleGroups() {
        // Arrange
        TripGroup group2 = new TripGroup();
        group2.setId("group2");
        group2.setName("Group 2");
        group2.setCreatedBy(user1);
        group2.setMembers(new HashSet<>(Arrays.asList(user1, user2)));

        Expense expense1 = new Expense();
        expense1.setId(1L);
        expense1.setDescription("Dinner Group 1");
        expense1.setAmount(new BigDecimal("60.00"));
        expense1.setPaidBy(user2);
        expense1.setTripGroup(tripGroup);

        ExpenseParticipant participant1_1 = new ExpenseParticipant();
        participant1_1.setUser(user1);
        participant1_1.setAmount(new BigDecimal("60.00"));
        participant1_1.setIsPaid(false);
        participant1_1.setExpense(expense1);
        expense1.setParticipants(new HashSet<>(Arrays.asList(participant1_1)));

        Expense expense2 = new Expense();
        expense2.setId(2L);
        expense2.setDescription("Lunch Group 2");
        expense2.setAmount(new BigDecimal("40.00"));
        expense2.setPaidBy(user2);
        expense2.setTripGroup(group2);

        ExpenseParticipant participant2_1 = new ExpenseParticipant();
        participant2_1.setUser(user1);
        participant2_1.setAmount(new BigDecimal("40.00"));
        participant2_1.setIsPaid(false);
        participant2_1.setExpense(expense2);
        expense2.setParticipants(new HashSet<>(Arrays.asList(participant2_1)));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(expenseParticipantRepository.findByUser(user1)).thenReturn(
            Arrays.asList(participant1_1, participant2_1)
        );
        when(settlementRepository.findByPayer(user1)).thenReturn(new ArrayList<>());
        when(settlementRepository.findByPayee(user1)).thenReturn(new ArrayList<>());
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        // Act
        List<com.splitpush.dto.BalanceDTO> balances = expenseService.calculateBalances(1L);

        // Assert
        assertNotNull(balances);
        assertEquals(1, balances.size());
        
        com.splitpush.dto.BalanceDTO balance = balances.get(0);
        assertEquals(2L, balance.getUserId());
        assertEquals(new BigDecimal("-100.00"), balance.getNetBalance()); // -60 - 40 = -100
        
        // Check group breakdown
        assertNotNull(balance.getGroupBreakdown());
        assertEquals(2, balance.getGroupBreakdown().size());
        assertEquals(new BigDecimal("-60.00"), balance.getGroupBreakdown().get("Test Group"));
        assertEquals(new BigDecimal("-40.00"), balance.getGroupBreakdown().get("Group 2"));
    }
}

