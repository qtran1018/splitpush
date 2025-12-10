let groupMembers = [];
let currentGroup = null;
let currentUser = null;

async function loadCurrentUser() {
    try {
        const response = await fetch('/api/users/me');
        if (response.ok) {
            currentUser = await response.json();
        }
    } catch (error) {
        console.error('Error loading current user:', error);
    }
}

async function loadGroup() {
    if (!groupId) {
        window.location.href = '/groups';
        return;
    }

    try {
        // Load current user first
        await loadCurrentUser();
        
        const response = await fetch(`/api/groups/${groupId}`);
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({ error: 'Unknown error' }));
            console.error('Failed to load group:', response.status, errorData);
            throw new Error(errorData.error || `Failed to load group (${response.status})`);
        }
        
        currentGroup = await response.json();
        groupMembers = currentGroup.members || [];
        
        document.getElementById('group-name').textContent = currentGroup.name;
        loadExpenses();
    } catch (error) {
        console.error('Error loading group:', error);
        alert('Failed to load group: ' + error.message + '. Redirecting...');
        window.location.href = '/groups';
    }
}

let currentPage = 0;
const pageSize = 10;

async function loadExpenses(page = 0) {
    console.log(`[CACHE DEBUG] Frontend: Requesting expenses for group ${groupId}, page ${page}, size ${pageSize}`);
    const startTime = performance.now();
    try {
        const response = await fetch(`/api/expenses/group/${groupId}?page=${page}&size=${pageSize}`);
        const endTime = performance.now();
        const duration = (endTime - startTime).toFixed(2);
        if (!response.ok) {
            throw new Error('Failed to load expenses');
        }
        
        const data = await response.json();
        const expenses = data.content || [];
        console.log(`[CACHE DEBUG] Frontend: Received ${expenses.length} expenses from API in ${duration}ms`);
        console.log(`[CACHE DEBUG] Frontend: Check server logs - if you see "CACHE MISS" logs, caching is NOT working. If you DON'T see server logs on subsequent requests, caching IS working.`);
        const container = document.getElementById('expenses-container');
        const noExpenses = document.getElementById('no-expenses');
        const paginationContainer = document.getElementById('pagination-container');
        
        if (expenses.length === 0 && page === 0) {
            container.style.display = 'none';
            noExpenses.style.display = 'block';
            if (paginationContainer) paginationContainer.style.display = 'none';
            return;
        }
        
        container.style.display = 'flex';
        noExpenses.style.display = 'none';
        currentPage = data.currentPage || 0;
        
        container.innerHTML = expenses.map(expense => {
            const paidBy = expense.paidBy.name || expense.paidBy.username;
            const participants = expense.participants || [];
            const participantList = participants.map(p => {
                const userName = p.user.name || p.user.username;
                const status = p.isPaid ? ' (Paid)' : '';
                return `<div class="participant-item">
                    <span>${userName}</span>
                    <span>$${parseFloat(p.amount).toFixed(2)}${status}</span>
                </div>`;
            }).join('');
            
            // Format date in local timezone
            // LocalDateTime is serialized as ISO string (e.g., "2023-12-09T01:47:07")
            // We parse it and display in user's local timezone
            let dateDisplay = '';
            if (expense.createdAt) {
                try {
                    // Parse ISO string - if no timezone, treat as UTC and convert to local
                    const dateStr = expense.createdAt;
                    let date;
                    if (dateStr.includes('Z') || dateStr.includes('+') || dateStr.includes('-') && dateStr.length > 19) {
                        // Has timezone info
                        date = new Date(dateStr);
                    } else {
                        // No timezone - assume UTC and convert to local
                        date = new Date(dateStr + 'Z');
                    }
                    dateDisplay = `<p class="expense-date">${date.toLocaleDateString()} ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</p>`;
                } catch (e) {
                    console.error('Error parsing date:', e);
                }
            }
            
            return `
                <div class="expense-card" data-expense-id="${expense.id}">
                    <div class="expense-header">
                        <div class="expense-info">
                            <h3>${expense.description}</h3>
                            <p>Paid by ${paidBy}</p>
                            ${dateDisplay}
                        </div>
                        <div class="expense-amount">$${parseFloat(expense.amount).toFixed(2)}</div>
                    </div>
                    <div class="expense-participants">
                        <strong>Split among:</strong>
                        ${participantList}
                    </div>
                    <div class="expense-actions">
                        <button class="btn btn-secondary btn-sm" onclick="editExpense(${expense.id})">Edit</button>
                        <button class="btn btn-danger btn-sm" onclick="deleteExpense(${expense.id})">Delete</button>
                    </div>
                </div>
            `;
        }).join('');
        
        // Render pagination controls
        if (paginationContainer) {
            renderPagination(data, paginationContainer);
        }
    } catch (error) {
        console.error('Error loading expenses:', error);
        document.getElementById('expenses-container').innerHTML = 
            '<div class="alert alert-error">Failed to load expenses. Please try again.</div>';
    }
}

function renderPagination(data, container) {
    const totalPages = data.totalPages || 0;
    const currentPageNum = data.currentPage || 0;
    const hasNext = data.hasNext || false;
    const hasPrevious = data.hasPrevious || false;
    
    if (totalPages <= 1) {
        container.style.display = 'none';
        return;
    }
    
    container.style.display = 'flex';
    
    let paginationHTML = '<div class="pagination">';
    
    // Previous button
    if (hasPrevious) {
        paginationHTML += `<button class="btn btn-secondary btn-sm" onclick="loadExpenses(${currentPageNum - 1})">Previous</button>`;
    } else {
        paginationHTML += `<button class="btn btn-secondary btn-sm" disabled>Previous</button>`;
    }
    
    // Page numbers
    paginationHTML += `<span class="pagination-info">Page ${currentPageNum + 1} of ${totalPages}</span>`;
    
    // Next button
    if (hasNext) {
        paginationHTML += `<button class="btn btn-secondary btn-sm" onclick="loadExpenses(${currentPageNum + 1})">Next</button>`;
    } else {
        paginationHTML += `<button class="btn btn-secondary btn-sm" disabled>Next</button>`;
    }
    
    paginationHTML += '</div>';
    container.innerHTML = paginationHTML;
}

function showCreateExpenseModal() {
    const modal = document.getElementById('create-expense-modal');
    const paidBySelect = document.getElementById('expense-paid-by');
    const participantsList = document.getElementById('participants-list');
    
    // Populate paid by dropdown
    paidBySelect.innerHTML = groupMembers.map(member => 
        `<option value="${member.id}">${member.name || member.username}</option>`
    ).join('');
    
    // Set default to current user if available
    if (currentUser) {
        const currentUserOption = Array.from(paidBySelect.options).find(opt => 
            parseInt(opt.value) === currentUser.id
        );
        if (currentUserOption) {
            paidBySelect.value = currentUser.id;
        }
    }
    
    // Populate participants list
    participantsList.innerHTML = groupMembers.map(member => {
        const memberName = member.name || member.username;
        return `
            <div class="participant-split">
                <label>
                    <input type="checkbox" class="participant-checkbox" value="${member.id}" checked>
                    ${memberName}
                </label>
                <input type="number" class="participant-amount" data-user-id="${member.id}" 
                       step="0.01" min="0" value="0">
            </div>
        `;
    }).join('');
    
    // Enable amount input when checkbox is checked
    participantsList.querySelectorAll('.participant-checkbox').forEach(checkbox => {
        // Set initial state based on checkbox checked status
        const amountInput = participantsList.querySelector(`.participant-amount[data-user-id="${checkbox.value}"]`);
        amountInput.disabled = !checkbox.checked;
        
        checkbox.addEventListener('change', function() {
            const amountInput = participantsList.querySelector(`.participant-amount[data-user-id="${this.value}"]`);
            amountInput.disabled = !this.checked;
            if (!this.checked) {
                amountInput.value = '0';
            }
        });
    });
    
    modal.classList.add('active');
    modal.style.display = 'flex';
}

/**
 * Split expense equally using Largest Remainder Method with integer cents.
 * This ensures the total always sums to the original amount without floating-point errors.
 */
function splitEqually() {
    const totalAmount = parseFloat(document.getElementById('expense-amount').value) || 0;
    const checkedBoxes = Array.from(document.querySelectorAll('.participant-checkbox:checked'));
    
    if (checkedBoxes.length === 0 || totalAmount === 0) {
        alert('Please enter an amount and select at least one participant');
        return;
    }
    
    // Convert to integer cents to avoid floating-point errors
    const totalCents = Math.round(totalAmount * 100);
    const numParticipants = checkedBoxes.length;
    
    // Calculate base amount per person in cents (truncated)
    const baseCentsPerPerson = Math.floor(totalCents / numParticipants);
    
    // Calculate remainder (leftover cents)
    const remainderCents = totalCents % numParticipants;
    
    // Create array of participant amounts in cents
    const participantAmounts = checkedBoxes.map((checkbox, index) => {
        // Distribute remainder cents to first N participants (one cent each)
        const cents = baseCentsPerPerson + (index < remainderCents ? 1 : 0);
        return {
            userId: checkbox.value,
            cents: cents
        };
    });
    
    // Set amounts in the form (convert back to dollars with 2 decimal places)
    participantAmounts.forEach(({ userId, cents }) => {
        const amountInput = document.querySelector(`.participant-amount[data-user-id="${userId}"]`);
        const amountInDollars = (cents / 100).toFixed(2);
        amountInput.value = amountInDollars;
    });
}

function selectAllParticipants() {
    const participantsList = document.getElementById('participants-list');
    if (!participantsList) return;

    participantsList.querySelectorAll('.participant-checkbox').forEach(checkbox => {
        checkbox.checked = true;
        const amountInput = participantsList.querySelector(`.participant-amount[data-user-id="${checkbox.value}"]`);
        if (amountInput) {
            amountInput.disabled = false;
        }
    });
}

function deselectAllParticipants() {
    const participantsList = document.getElementById('participants-list');
    if (!participantsList) return;

    participantsList.querySelectorAll('.participant-checkbox').forEach(checkbox => {
        checkbox.checked = false;
        const amountInput = participantsList.querySelector(`.participant-amount[data-user-id="${checkbox.value}"]`);
        if (amountInput) {
            amountInput.disabled = true;
            amountInput.value = '0';
        }
    });
}

// Single form submit handler (removed duplicate)

// Close modal when clicking outside
window.onclick = function(event) {
    const modal = document.getElementById('create-expense-modal');
    if (event.target === modal) {
        closeCreateExpenseModal();
    }
}

async function editExpense(expenseId) {
    try {
        const response = await fetch(`/api/expenses/${expenseId}`);
        if (!response.ok) throw new Error('Failed to load expense');
        
        const expense = await response.json();
        showEditExpenseModal(expense);
    } catch (error) {
        console.error('Error loading expense:', error);
        alert('Failed to load expense details');
    }
}

function showEditExpenseModal(expense) {
    const modal = document.getElementById('create-expense-modal');
    const form = document.getElementById('create-expense-form');
    const modalTitle = modal.querySelector('h3');
    const submitButton = form.querySelector('button[type="submit"]');
    const paidBySelect = document.getElementById('expense-paid-by');
    const participantsList = document.getElementById('participants-list');
    
    modalTitle.textContent = 'Edit Expense';
    submitButton.textContent = 'Save Expense';
    form.dataset.expenseId = expense.id;
    
    // Populate paid by dropdown fresh to ensure options exist
    paidBySelect.innerHTML = groupMembers.map(member =>
        `<option value="${member.id}">${member.name || member.username}</option>`
    ).join('');

    document.getElementById('expense-description').value = expense.description;
    document.getElementById('expense-amount').value = expense.amount;
    paidBySelect.value = expense.paidBy.id;
    
    // Populate participants
    participantsList.innerHTML = groupMembers.map(member => {
        const memberName = member.name || member.username;
        const participant = expense.participants.find(p => p.user.id === member.id);
        const isChecked = !!participant;
        const amount = participant ? participant.amount : 0;
        
        return `
            <div class="participant-split">
                <label>
                    <input type="checkbox" class="participant-checkbox" value="${member.id}" ${isChecked ? 'checked' : ''}>
                    ${memberName}
                </label>
                <input type="number" class="participant-amount" data-user-id="${member.id}" 
                       step="0.01" min="0" value="${amount}" ${isChecked ? '' : 'disabled'}>
            </div>
        `;
    }).join('');
    
    // Enable amount input when checkbox is checked
    participantsList.querySelectorAll('.participant-checkbox').forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            const amountInput = participantsList.querySelector(`.participant-amount[data-user-id="${this.value}"]`);
            amountInput.disabled = !this.checked;
            if (!this.checked) {
                amountInput.value = '0';
            }
        });
    });
    
    modal.classList.add('active');
    modal.style.display = 'flex';
}

async function deleteExpense(expenseId) {
    if (!confirm('Are you sure you want to delete this expense?')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/expenses/${expenseId}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            // Reload current page after deletion
            loadExpenses(currentPage);
        } else {
            const data = await response.json();
            alert('Error: ' + (data.error || 'Failed to delete expense'));
        }
    } catch (error) {
        console.error('Error deleting expense:', error);
        alert('An error occurred. Please try again.');
    }
}

function closeCreateExpenseModal() {
    const modal = document.getElementById('create-expense-modal');
    const form = document.getElementById('create-expense-form');
    const modalTitle = modal.querySelector('h3');
    const submitButton = form.querySelector('button[type="submit"]');
    
    modal.classList.remove('active');
    modal.style.display = 'none';
    form.reset();
    delete form.dataset.expenseId;
    modalTitle.textContent = 'Add Expense';
    submitButton.textContent = 'Add Expense';
}

// Update form submit handler to support both create and edit
document.getElementById('create-expense-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const form = e.target;
    const expenseId = form.dataset.expenseId;
    const isEdit = !!expenseId;
    
    const description = document.getElementById('expense-description').value;
    const amount = parseFloat(document.getElementById('expense-amount').value);
    const paidByUserId = parseInt(document.getElementById('expense-paid-by').value);
    
    const participantAmounts = {};
    document.querySelectorAll('.participant-checkbox:checked').forEach(checkbox => {
        const userId = parseInt(checkbox.value);
        const amountInput = document.querySelector(`.participant-amount[data-user-id="${userId}"]`);
        const participantAmount = parseFloat(amountInput.value) || 0;
        
        if (participantAmount > 0) {
            participantAmounts[userId] = participantAmount;
        }
    });
    
    // Validate that participant amounts sum to total
    const totalParticipantAmount = Object.values(participantAmounts).reduce((sum, amt) => sum + amt, 0);
    if (Math.abs(totalParticipantAmount - amount) > 0.01) {
        alert(`Participant amounts ($${totalParticipantAmount.toFixed(2)}) must equal total amount ($${amount.toFixed(2)})`);
        return;
    }
    
    const expenseData = {
        description,
        amount,
        paidByUserId,
        tripGroupId: groupId, // ULID is a string
        participantAmounts
    };
    
    try {
        const url = isEdit ? `/api/expenses/${expenseId}` : '/api/expenses';
        const method = isEdit ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(expenseData)
        });
        
        if (response.ok) {
            closeCreateExpenseModal();
            // Reload current page (or go to first page if editing/creating)
            loadExpenses(isEdit ? currentPage : 0);
        } else {
            const data = await response.json();
            alert('Error: ' + (data.error || `Failed to ${isEdit ? 'update' : 'create'} expense`));
        }
    } catch (error) {
        console.error(`Error ${isEdit ? 'updating' : 'creating'} expense:`, error);
        alert('An error occurred. Please try again.');
    }
});

// Load group and expenses on page load
document.addEventListener('DOMContentLoaded', loadGroup);

