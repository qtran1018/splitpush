async function loadBalances() {
    try {
        const response = await fetch('/api/dashboard/balances');
        if (!response.ok) {
            throw new Error('Failed to load balances');
        }
        
        const balances = await response.json();
        const container = document.getElementById('balances-container');
        const noBalances = document.getElementById('no-balances');
        
        if (balances.length === 0) {
            container.style.display = 'none';
            noBalances.style.display = 'block';
            return;
        }
        
        container.style.display = 'grid';
        noBalances.style.display = 'none';
        
        container.innerHTML = balances.map(balance => {
            const isPositive = balance.netBalance > 0;
            const amountClass = isPositive ? 'balance-positive' : 'balance-negative';
            const message = isPositive 
                ? `${balance.name} owes you` 
                : `You owe ${balance.name}`;
            const amount = Math.abs(balance.netBalance).toFixed(2);
            const sign = isPositive ? '+' : '-';

            const breakdownDetails = balance.groupBreakdownDetails || [];
            const hasDetails = breakdownDetails.length > 0;
            const breakdown = balance.groupBreakdown || {};

            const breakdownHtml = hasDetails
                ? breakdownDetails.map(item => {
                    const sign = item.amount >= 0 ? '' : '-';
                    const absVal = Math.abs(item.amount).toFixed(2);
                    const link = item.groupId ? `<a class="group-link" href="/expenses?groupId=${item.groupId}" title="View expenses for ${item.groupName}">↗</a>` : '';
                    return `<div class="balance-breakdown-row">
                        <span>${item.groupName} ${link}</span>
                        <span>${sign}$${absVal}</span>
                    </div>`;
                }).join('')
                : Object.entries(breakdown).map(([groupName, value]) => {
                    const sign = value >= 0 ? '' : '-';
                    const absVal = Math.abs(value).toFixed(2);
                    const groupMatch = userGroups.find(g => g.name === groupName);
                    const link = groupMatch ? `<a class="group-link" href="/expenses?groupId=${groupMatch.id}" title="View expenses for ${groupName}">↗</a>` : '';
                    return `<div class="balance-breakdown-row">
                        <span>${groupName} ${link}</span>
                        <span>${sign}$${absVal}</span>
                    </div>`;
                }).join('');
            
            // Add settlement button if user owes or is owed
            const settlementButton = balance.netBalance !== 0 ? 
                `<button class="btn btn-secondary btn-sm record-settlement-btn"
                    data-payee-id="${balance.userId}"
                    data-payee-name="${(balance.name || '').replace(/"/g, '&quot;')}"
                    data-breakdown='${JSON.stringify(breakdown).replace(/'/g, "&#39;")}'
                    data-breakdown-details='${JSON.stringify(breakdownDetails).replace(/'/g, "&#39;")}'
                    onclick="event.stopPropagation();">Record Settlement</button>` : 
                '';
            
            return `
                <div class="balance-card">
                    <div class="balance-info">
                        <h3>${balance.name}</h3>
                        <p>${message}</p>
                        ${breakdownHtml ? `<div class="balance-breakdown">${breakdownHtml}</div>` : ''}
                        ${settlementButton ? `<div class="balance-actions" style="margin-top: 10px;">${settlementButton}</div>` : ''}
                    </div>
                    <div class="balance-amount ${amountClass}">
                        ${sign}$${amount}
                    </div>
                </div>
            `;
        }).join('');
        
        // Attach event listeners to settlement buttons
        container.querySelectorAll('.record-settlement-btn').forEach(btn => {
            btn.addEventListener('click', function(e) {
                e.stopPropagation();
                const payeeId = parseInt(this.getAttribute('data-payee-id'));
                const payeeName = this.getAttribute('data-payee-name');
                const breakdownJson = this.getAttribute('data-breakdown');
                const breakdownDetailsJson = this.getAttribute('data-breakdown-details');
                let breakdown = {};
                let breakdownDetails = [];
                try {
                    breakdown = JSON.parse(breakdownJson.replace(/&#39;/g, "'"));
                } catch (err) {
                    console.error('Error parsing breakdown:', err);
                }
                try {
                    breakdownDetails = JSON.parse(breakdownDetailsJson.replace(/&#39;/g, "'"));
                } catch (err) {
                    breakdownDetails = [];
                }
                window.showSettlementModal(payeeId, payeeName, breakdown, breakdownDetails);
            });
        });
    } catch (error) {
        console.error('Error loading balances:', error);
        document.getElementById('balances-container').innerHTML = 
            '<div class="alert alert-error">Failed to load balances. Please try again.</div>';
    }
}

let userGroups = [];

async function loadUserGroups() {
    try {
        const response = await fetch('/api/groups');
        if (response.ok) {
            userGroups = await response.json();
        }
    } catch (error) {
        console.error('Error loading groups:', error);
    }
}

window.showSettlementModal = function(payeeUserId, payeeName, breakdown, breakdownDetails = []) {
    try {
        const modal = document.getElementById('settlement-modal');
        if (!modal) {
            console.error('Settlement modal not found');
            alert('Settlement modal not found. Please refresh the page.');
            return;
        }
        
        const groupSelect = document.getElementById('settlement-group');
        const payeeDisplay = document.getElementById('settlement-payee-display');
        
        if (!groupSelect || !payeeDisplay) {
            console.error('Settlement form elements not found');
            alert('Form elements not found. Please refresh the page.');
            return;
        }
        
        document.getElementById('settlement-payee-id').value = payeeUserId;
        document.getElementById('settlement-payee-name').value = payeeName;
        payeeDisplay.textContent = payeeName;
        
        // Ensure userGroups is loaded
        if (!userGroups || userGroups.length === 0) {
            console.warn('User groups not loaded, loading now...');
            loadUserGroups().then(() => {
                const hasGroups = populateSettlementGroups(groupSelect, breakdown, breakdownDetails);
                if (hasGroups) {
                    modal.classList.add('active');
                    modal.style.display = 'flex';
                }
            });
            return;
        }
        
        const hasGroups = populateSettlementGroups(groupSelect, breakdown, breakdownDetails);
        if (!hasGroups) {
            // Don't open modal if no groups available
            return;
        }
        
        modal.classList.add('active');
        modal.style.display = 'flex';
    } catch (error) {
        console.error('Error showing settlement modal:', error);
        alert('An error occurred. Please try again.');
    }
};

function populateSettlementGroups(groupSelect, breakdown, breakdownDetails = []) {
    // Prefer breakdownDetails (contains stable group IDs). Fallback to name-based map for legacy.
    let options = '';

    if (breakdownDetails && breakdownDetails.length > 0) {
        options = breakdownDetails
            .filter(item => item.amount < 0) // Only groups where user owes money
            .map(item => `<option value="${item.groupId}">${item.groupName} ($${Math.abs(item.amount).toFixed(2)})</option>`)
            .join('');
    } else {
        options = Object.entries(breakdown)
            .filter(([groupName, value]) => value < 0)
            .map(([groupName, value]) => {
                const group = userGroups.find(g => g.name === groupName);
                if (group) {
                    return `<option value="${group.id}">${groupName} ($${Math.abs(value).toFixed(2)})</option>`;
                }
                return '';
            })
            .filter(opt => opt !== '')
            .join('');
    }

    groupSelect.innerHTML = options;

    if (groupSelect.options.length === 0) {
        alert('No groups where you owe money. You can only record settlements for amounts you owe.');
        return false;
    }
    return true;
}

function closeSettlementModal() {
    const modal = document.getElementById('settlement-modal');
    modal.classList.remove('active');
    modal.style.display = 'none';
    document.getElementById('settlement-form').reset();
}

document.getElementById('settlement-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const payeeUserId = parseInt(document.getElementById('settlement-payee-id').value);
    const tripGroupId = document.getElementById('settlement-group').value;
    const amount = parseFloat(document.getElementById('settlement-amount').value);
    
    const settlementData = {
        payeeUserId: payeeUserId,
        tripGroupId: tripGroupId,
        amount: amount
    };
    
    try {
        const response = await fetch('/api/settlements', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(settlementData)
        });
        
        if (response.ok) {
            closeSettlementModal();
            loadBalances();
        } else {
            const data = await response.json();
            alert('Error: ' + (data.error || 'Failed to record settlement'));
        }
    } catch (error) {
        console.error('Error recording settlement:', error);
        alert('An error occurred. Please try again.');
    }
});

// Close modal when clicking outside
window.onclick = function(event) {
    const modal = document.getElementById('settlement-modal');
    if (event.target === modal) {
        closeSettlementModal();
    }
}

// Load balances and groups on page load
document.addEventListener('DOMContentLoaded', async () => {
    await loadUserGroups();
    loadBalances();
});

