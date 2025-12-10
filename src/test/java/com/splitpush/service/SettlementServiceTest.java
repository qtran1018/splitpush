package com.splitpush.service;

import com.splitpush.dto.SettlementDTO;
import com.splitpush.model.Settlement;
import com.splitpush.model.TripGroup;
import com.splitpush.model.User;
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
class SettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private TripGroupRepository tripGroupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExpenseService expenseService;

    @InjectMocks
    private SettlementService settlementService;

    private User user1;
    private User user2;
    private TripGroup tripGroup;
    private SettlementDTO settlementDTO;

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

        // Create test trip group
        tripGroup = new TripGroup();
        tripGroup.setId("group1");
        tripGroup.setName("Test Group");
        tripGroup.setDescription("Test Description");
        tripGroup.setCreatedBy(user1);
        tripGroup.setMembers(new HashSet<>(Arrays.asList(user1, user2)));

        // Create settlement DTO
        settlementDTO = new SettlementDTO();
        settlementDTO.setPayeeUserId(2L);
        settlementDTO.setTripGroupId("group1");
        settlementDTO.setAmount(new BigDecimal("50.00"));
    }

    @Test
    void testCreateSettlement_Success() {
        // Arrange
        // user1 owes user2 $100, so user1 can pay $50
        when(tripGroupRepository.findById("group1")).thenReturn(Optional.of(tripGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(expenseService.calculateBalanceForGroup(1L, 2L, "group1"))
            .thenReturn(new BigDecimal("-100.00")); // user1 owes user2 $100

        Settlement savedSettlement = new Settlement();
        savedSettlement.setId(1L);
        savedSettlement.setPayer(user1);
        savedSettlement.setPayee(user2);
        savedSettlement.setAmount(new BigDecimal("50.00"));
        savedSettlement.setTripGroup(tripGroup);

        when(settlementRepository.save(any(Settlement.class))).thenReturn(savedSettlement);

        // Act
        Settlement result = settlementService.createSettlement(settlementDTO, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(user1.getId(), result.getPayer().getId());
        assertEquals(user2.getId(), result.getPayee().getId());
        assertEquals(new BigDecimal("50.00"), result.getAmount());
        assertEquals("group1", result.getTripGroup().getId());
        
        verify(settlementRepository, times(1)).save(any(Settlement.class));
    }

    @Test
    void testCreateSettlement_AmountExceedsOwed_ThrowsException() {
        // Arrange
        // user1 owes user2 $50, but tries to pay $100
        when(tripGroupRepository.findById("group1")).thenReturn(Optional.of(tripGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(expenseService.calculateBalanceForGroup(1L, 2L, "group1"))
            .thenReturn(new BigDecimal("-50.00")); // user1 owes user2 $50

        settlementDTO.setAmount(new BigDecimal("100.00")); // Trying to pay more than owed

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            settlementService.createSettlement(settlementDTO, 1L);
        });
        
        assertTrue(exception.getMessage().contains("Settlement amount") && 
                   exception.getMessage().contains("exceeds amount owed"));
    }

    @Test
    void testCreateSettlement_NoDebt_ThrowsException() {
        // Arrange
        // user1 doesn't owe user2 anything (balance is positive or zero)
        when(tripGroupRepository.findById("group1")).thenReturn(Optional.of(tripGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(expenseService.calculateBalanceForGroup(1L, 2L, "group1"))
            .thenReturn(new BigDecimal("10.00")); // user2 owes user1 $10 (positive balance)

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            settlementService.createSettlement(settlementDTO, 1L);
        });
        
        assertTrue(exception.getMessage().contains("You don't owe this user anything"));
    }

    @Test
    void testCreateSettlement_UserNotMember_ThrowsException() {
        // Arrange
        User user3 = new User();
        user3.setId(3L);
        user3.setUsername("user3");
        
        TripGroup groupWithoutUser3 = new TripGroup();
        groupWithoutUser3.setId("group1");
        groupWithoutUser3.setName("Test Group");
        groupWithoutUser3.setMembers(new HashSet<>(Arrays.asList(user1, user2))); // user3 not a member

        when(tripGroupRepository.findById("group1")).thenReturn(Optional.of(groupWithoutUser3));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        settlementDTO.setPayeeUserId(2L);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            settlementService.createSettlement(settlementDTO, 3L); // user3 not a member
        });
        
        assertTrue(exception.getMessage().contains("must be members of the trip group"));
    }

    @Test
    void testCreateSettlement_PayerAndPayeeSame_ThrowsException() {
        // Arrange
        when(tripGroupRepository.findById("group1")).thenReturn(Optional.of(tripGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

        settlementDTO.setPayeeUserId(1L); // Same as payer

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            settlementService.createSettlement(settlementDTO, 1L);
        });
        
        assertTrue(exception.getMessage().contains("Payer and payee cannot be the same user"));
    }

    @Test
    void testCreateSettlement_ExactAmountOwed() {
        // Arrange
        // user1 owes user2 exactly $50, pays $50
        when(tripGroupRepository.findById("group1")).thenReturn(Optional.of(tripGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(expenseService.calculateBalanceForGroup(1L, 2L, "group1"))
            .thenReturn(new BigDecimal("-50.00")); // user1 owes user2 exactly $50

        settlementDTO.setAmount(new BigDecimal("50.00")); // Paying exactly what's owed

        Settlement savedSettlement = new Settlement();
        savedSettlement.setId(1L);
        savedSettlement.setPayer(user1);
        savedSettlement.setPayee(user2);
        savedSettlement.setAmount(new BigDecimal("50.00"));
        savedSettlement.setTripGroup(tripGroup);

        when(settlementRepository.save(any(Settlement.class))).thenReturn(savedSettlement);

        // Act
        Settlement result = settlementService.createSettlement(settlementDTO, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("50.00"), result.getAmount());
        verify(settlementRepository, times(1)).save(any(Settlement.class));
    }

    @Test
    void testGetSettlementsByGroup() {
        // Arrange
        Settlement settlement1 = new Settlement();
        settlement1.setId(1L);
        settlement1.setPayer(user1);
        settlement1.setPayee(user2);
        settlement1.setAmount(new BigDecimal("50.00"));
        settlement1.setTripGroup(tripGroup);

        Settlement settlement2 = new Settlement();
        settlement2.setId(2L);
        settlement2.setPayer(user2);
        settlement2.setPayee(user1);
        settlement2.setAmount(new BigDecimal("30.00"));
        settlement2.setTripGroup(tripGroup);

        when(tripGroupRepository.findById("group1")).thenReturn(Optional.of(tripGroup));
        when(settlementRepository.findByTripGroup(tripGroup))
            .thenReturn(Arrays.asList(settlement1, settlement2));

        // Act
        List<Settlement> settlements = settlementService.getSettlementsByGroup("group1");

        // Assert
        assertNotNull(settlements);
        assertEquals(2, settlements.size());
    }

    @Test
    void testGetSettlementsByUser() {
        // Arrange
        Settlement settlement1 = new Settlement();
        settlement1.setId(1L);
        settlement1.setPayer(user1);
        settlement1.setPayee(user2);
        settlement1.setAmount(new BigDecimal("50.00"));
        settlement1.setTripGroup(tripGroup);

        Settlement settlement2 = new Settlement();
        settlement2.setId(2L);
        settlement2.setPayer(user2);
        settlement2.setPayee(user1);
        settlement2.setAmount(new BigDecimal("30.00"));
        settlement2.setTripGroup(tripGroup);

        List<Settlement> payerList = new ArrayList<>();
        payerList.add(settlement1);
        List<Settlement> payeeList = new ArrayList<>();
        payeeList.add(settlement2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(settlementRepository.findByPayer(user1)).thenReturn(payerList);
        when(settlementRepository.findByPayee(user1)).thenReturn(payeeList);

        // Act
        List<Settlement> settlements = settlementService.getSettlementsByUser(1L);

        // Assert
        assertNotNull(settlements);
        assertEquals(2, settlements.size());
    }
}

