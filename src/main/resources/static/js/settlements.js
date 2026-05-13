let currentUser = null;
let settlementsData = [];
let filteredSettlementsData = [];
let settlementsPage = 0;
const settlementsPageSize = 10;
let searchFilter = '';

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

function filterSettlements() {
    if (!searchFilter.trim()) {
        filteredSettlementsData = settlementsData;
    } else {
        const searchLower = searchFilter.toLowerCase().trim();
        filteredSettlementsData = settlementsData.filter(settlement => {
            const groupName = (settlement.tripGroup?.name || '').toLowerCase();
            const groupId = (settlement.tripGroup?.id || '').toString().toLowerCase();
            return groupName.includes(searchLower) || groupId.includes(searchLower);
        });
    }
    // Reset to first page when filtering
    settlementsPage = 0;
    renderSettlements(0);
}

function renderSettlements(page = 0) {
    const container = document.getElementById('settlements-container');
    const noSettlements = document.getElementById('no-settlements');
    const paginationContainer = document.getElementById('settlements-pagination');

    if (!filteredSettlementsData || filteredSettlementsData.length === 0) {
        container.style.display = 'none';
        noSettlements.style.display = 'block';
        if (searchFilter.trim()) {
            noSettlements.innerHTML = '<p>No settlements found matching your search.</p>';
        } else {
            noSettlements.innerHTML = '<p>No settlements recorded yet.</p>';
        }
        paginationContainer.style.display = 'none';
        return;
    }

    const totalPages = Math.ceil(filteredSettlementsData.length / settlementsPageSize);
    settlementsPage = Math.min(Math.max(page, 0), totalPages - 1);

    const start = settlementsPage * settlementsPageSize;
    const end = start + settlementsPageSize;
    const pageItems = filteredSettlementsData.slice(start, end);

    container.style.display = 'flex';
    noSettlements.style.display = 'none';

    container.innerHTML = pageItems.map(settlement => {
        const payer = settlement.payer;
        const payee = settlement.payee;
        const tripGroup = settlement.tripGroup;
        const amount = parseFloat(settlement.amount).toFixed(2);
        
        // Determine action text
        let actionText = '';
        if (payer.id === currentUser.id) {
            // User paid someone
            const otherUser = payee.name || payee.username;
            actionText = `You paid ${otherUser} $${amount}`;
        } else {
            // User received payment
            const otherUser = payer.name || payer.username;
            actionText = `${otherUser} paid you $${amount}`;
        }
        
        // Format date in local timezone
        let dateDisplay = '';
        if (settlement.createdAt) {
            try {
                const date = new Date(settlement.createdAt);
                dateDisplay = date.toLocaleDateString() + ' ' + 
                             date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            } catch (e) {
                console.error('Error parsing date:', e);
            }
        }
        
        return `
            <div class="settlement-card">
                <div class="settlement-header">
                    <div class="settlement-info">
                        <h3>${tripGroup.name}</h3>
                        <p class="settlement-action">${actionText}</p>
                        <p class="settlement-meta">
                            Group ID: <code>${tripGroup.id}</code> • ${dateDisplay}
                        </p>
                    </div>
                    <div class="settlement-amount">
                        $${amount}
                    </div>
                </div>
            </div>
        `;
    }).join('');

    // Render pagination
    if (totalPages > 1) {
        paginationContainer.style.display = 'flex';
        let paginationHTML = '<div class="pagination">';

        if (settlementsPage > 0) {
            paginationHTML += `<button class="btn btn-secondary btn-sm" onclick="changeSettlementsPage(${settlementsPage - 1})">Previous</button>`;
        } else {
            paginationHTML += `<button class="btn btn-secondary btn-sm" disabled>Previous</button>`;
        }

        paginationHTML += `<span class="pagination-info">Page ${settlementsPage + 1} of ${totalPages}</span>`;

        if (settlementsPage < totalPages - 1) {
            paginationHTML += `<button class="btn btn-secondary btn-sm" onclick="changeSettlementsPage(${settlementsPage + 1})">Next</button>`;
        } else {
            paginationHTML += `<button class="btn btn-secondary btn-sm" disabled>Next</button>`;
        }

        paginationHTML += '</div>';
        paginationContainer.innerHTML = paginationHTML;
    } else {
        paginationContainer.style.display = 'none';
        paginationContainer.innerHTML = '';
    }
}

function changeSettlementsPage(page) {
    renderSettlements(page);
}

async function loadSettlements() {
    try {
        const response = await fetch('/api/settlements');
        if (!response.ok) {
            throw new Error('Failed to load settlements');
        }
        
        const settlements = await response.json();
        
        // Sort by date (newest first)
        settlements.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

        settlementsData = settlements;
        filteredSettlementsData = settlements;
        renderSettlements(0);
    } catch (error) {
        console.error('Error loading settlements:', error);
        document.getElementById('settlements-container').innerHTML = 
            '<div class="alert alert-error">Failed to load settlements. Please try again.</div>';
        const paginationContainer = document.getElementById('settlements-pagination');
        if (paginationContainer) {
            paginationContainer.style.display = 'none';
        }
    }
}

// Load settlements on page load
document.addEventListener('DOMContentLoaded', async () => {
    await loadCurrentUser();
    loadSettlements();
    
    // Add search input event listener
    const searchInput = document.getElementById('settlement-search');
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            searchFilter = e.target.value;
            filterSettlements();
        });
    }
});

