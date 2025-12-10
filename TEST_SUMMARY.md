# Unit Tests Summary

## Overview
Comprehensive unit tests have been created for the key portions of the Splitpush application, focusing on:
- Expense creation and validation
- Balance calculations (addition/subtraction)
- Settlement creation and validation
- Complex scenarios with multiple expenses and groups

## Test Files Created

### 1. ExpenseServiceTest.java
Tests for the `ExpenseService` class covering:
- ✅ Creating expenses with equal splits
- ✅ Validating participant amounts sum to expense amount
- ✅ Calculating balances when user pays expenses
- ✅ Calculating balances when user participates in expenses
- ✅ Calculating balances with settlements
- ✅ Calculating balance for a specific group
- ✅ Multiple groups with group breakdown

### 2. SettlementServiceTest.java
Tests for the `SettlementService` class covering:
- ✅ Creating settlements successfully
- ✅ Validating settlement amount doesn't exceed what's owed
- ✅ Preventing settlements when user doesn't owe anything
- ✅ Validating both users are group members
- ✅ Preventing payer and payee from being the same user
- ✅ Exact amount settlements
- ✅ Getting settlements by group
- ✅ Getting settlements by user

### 3. BalanceCalculationTest.java
Comprehensive tests for balance calculation scenarios:
- ✅ User pays expense - others owe user
- ✅ User participates in expense - user owes others
- ✅ Multiple expenses - net balance calculation
- ✅ Settlements reducing debt
- ✅ Settlements fully paying debt
- ✅ Reverse settlements (payee pays payer)
- ✅ Group breakdown for multiple groups

## Key Test Scenarios

### Expense Addition/Subtraction
- When user1 pays $100 split 3 ways: user2 and user3 each owe user1 their share
- When user2 pays $100 and user1 participates: user1 owes user2 their share (negative balance)
- Multiple expenses net out correctly (e.g., user1 pays $100, user2 pays $60 → net balance)

### Balance Calculations
- Positive balance = others owe you
- Negative balance = you owe others
- Balances correctly account for who paid and who participated
- Settlements reduce outstanding balances correctly

### Settlement Logic
- Settlements can only be recorded for amounts you owe (negative balances)
- Settlement amount cannot exceed what's owed
- Settlements reduce debt: if you owe $100 and pay $50, you still owe $50
- Full settlements zero out balances

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ExpenseServiceTest
mvn test -Dtest=SettlementServiceTest
mvn test -Dtest=BalanceCalculationTest

# Run with verbose output
mvn test -X
```

## Test Coverage

The tests cover:
- ✅ Core business logic for expense splitting
- ✅ Balance calculation algorithms
- ✅ Settlement validation and creation
- ✅ Edge cases (zero balances, exact amounts, etc.)
- ✅ Multiple groups and group breakdowns
- ✅ Error handling and validation

## Notes

- Tests use Mockito for mocking repositories
- All monetary calculations use BigDecimal for precision
- Tests verify both positive and negative balance scenarios
- Group breakdown is tested to ensure proper per-group tracking

