let groups = [];
let currentUser = null;

async function loadCurrentUser() {
    try {
        const response = await fetch('/api/users/me');
        if (response.ok) {
            currentUser = await response.json();
            window.currentUserId = currentUser.id;
        }
    } catch (error) {
        console.error('Error loading current user:', error);
    }
}

async function loadGroups() {
    try {
        const response = await fetch('/api/groups');
        if (!response.ok) {
            throw new Error('Failed to load groups');
        }
        
        groups = await response.json();
        const container = document.getElementById('groups-container');
        const noGroups = document.getElementById('no-groups');
        
        if (groups.length === 0) {
            container.style.display = 'none';
            noGroups.style.display = 'block';
            return;
        }
        
        container.style.display = 'grid';
        noGroups.style.display = 'none';
        
        container.innerHTML = groups.map(group => {
            const memberCount = group.members ? group.members.length : 0;
            const expenseCount = group.expenses ? group.expenses.length : 0;
            const members = group.members || [];
            const memberList = members.map(m => m.name || m.username).join(', ');
            
            // Check if current user is a member of the group (all members can edit description)
            // Handle both cases: members as array of objects with id, or array of ids
            const isMember = currentUser && group.members && 
                group.members.some(m => (m.id || m) === currentUser.id);
            const descriptionEscaped = (group.description || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
            const nameEscaped = (group.name || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
            const editButton = isMember ? 
                `<button class="btn btn-secondary btn-sm edit-description-btn" data-group-id="${group.id}" data-group-name="${nameEscaped}" data-description="${descriptionEscaped}" onclick="event.stopPropagation();">Edit Group</button>` : 
                '';
            
            return `
                <div class="group-card">
                    <div onclick="viewGroup('${group.id}')" style="cursor: pointer;">
                        <h3>${group.name}</h3>
                        ${group.description ? `<p style="word-wrap: break-word; overflow-wrap: break-word; line-height: 1.4;">${group.description}</p>` : ''}
                        <div class="group-meta">
                            ${memberCount} member(s) • ${expenseCount} expense(s)
                        </div>
                        <div class="group-id-display">
                            Group ID: <code id="group-id-${group.id}">${group.id}</code>
                            <button class="btn-copy" onclick="event.stopPropagation(); copyGroupId('${group.id}')" title="Copy Group ID">
                                📋
                            </button>
                        </div>
                        ${memberList ? `<div class="group-members-preview">Members: ${memberList}</div>` : ''}
                    </div>
                    <div class="group-actions">
                        <!-- Add Member button hidden for now, may be used later -->
                        <!-- <button class="btn btn-secondary btn-sm" onclick="event.stopPropagation(); showAddMemberModal('${group.id}')">Add Member</button> -->
                        ${editButton}
                        <button class="btn btn-danger btn-sm" onclick="event.stopPropagation(); leaveGroup('${group.id}')">Leave Group</button>
                    </div>
                </div>
            `;
        }).join('');
        
        // Attach event listeners to edit description buttons
        container.querySelectorAll('.edit-description-btn').forEach(btn => {
            btn.addEventListener('click', function(e) {
                e.stopPropagation();
                const groupId = this.getAttribute('data-group-id');
                const groupName = this.getAttribute('data-group-name') || '';
                const description = this.getAttribute('data-description') || '';
                window.showEditDescriptionModal(groupId, groupName, description);
            });
        });
    } catch (error) {
        console.error('Error loading groups:', error);
        document.getElementById('groups-container').innerHTML = 
            '<div class="alert alert-error">Failed to load groups. Please try again.</div>';
    }
}

function viewGroup(groupId) {
    window.location.href = `/expenses?groupId=${encodeURIComponent(groupId)}`;
}

function showCreateGroupModal() {
    document.getElementById('create-group-modal').style.display = 'block';
}

function closeCreateGroupModal() {
    document.getElementById('create-group-modal').style.display = 'none';
    document.getElementById('create-group-form').reset();
}

document.getElementById('create-group-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const formData = {
        name: document.getElementById('group-name').value,
        description: document.getElementById('group-description').value || ''
    };

    try {
        const response = await fetch('/api/groups', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });

        if (response.ok) {
            closeCreateGroupModal();
            loadGroups();
        } else {
            const data = await response.json();
            alert('Error: ' + (data.error || 'Failed to create group'));
        }
    } catch (error) {
        console.error('Error creating group:', error);
        alert('An error occurred. Please try again.');
    }
});

function showJoinGroupModal() {
    document.getElementById('join-group-modal').style.display = 'block';
}

function closeJoinGroupModal() {
    document.getElementById('join-group-modal').style.display = 'none';
    document.getElementById('join-group-form').reset();
}

function showAddMemberModal(groupId) {
    document.getElementById('add-member-modal').style.display = 'block';
    document.getElementById('add-member-group-id').value = groupId;
    document.getElementById('member-search').value = '';
    document.getElementById('member-search-results').innerHTML = '';
}

function closeAddMemberModal() {
    document.getElementById('add-member-modal').style.display = 'none';
    document.getElementById('add-member-form').reset();
    document.getElementById('member-search-results').innerHTML = '';
}

// User search with debounce
let searchTimeout;

function setupMemberSearch() {
    const searchInput = document.getElementById('member-search');
    if (!searchInput) return;
    
    searchInput.addEventListener('input', function(e) {
    const query = e.target.value.trim();
    const resultsDiv = document.getElementById('member-search-results');
    
    clearTimeout(searchTimeout);
    
    if (query.length < 2) {
        resultsDiv.innerHTML = '';
        return;
    }
    
    searchTimeout = setTimeout(async () => {
        try {
            const response = await fetch(`/api/users/search?query=${encodeURIComponent(query)}`);
            if (!response.ok) throw new Error('Search failed');
            
            const users = await response.json();
            if (users.length === 0) {
                resultsDiv.innerHTML = '<div class="search-result-item">No users found</div>';
                return;
            }
            
            resultsDiv.innerHTML = users.map(user => `
                <div class="search-result-item" onclick="addMemberToGroup(${user.id}, '${user.username}', '${user.name || user.username}')">
                    <strong>${user.name || user.username}</strong> (@${user.username})
                </div>
            `).join('');
        } catch (error) {
            console.error('Error searching users:', error);
            resultsDiv.innerHTML = '<div class="search-result-item error">Error searching users</div>';
        }
    }, 300);
    });
}

function showAddMemberModal(groupId) {
    document.getElementById('add-member-modal').style.display = 'block';
    document.getElementById('add-member-group-id').value = groupId;
    document.getElementById('member-search').value = '';
    document.getElementById('member-search-results').innerHTML = '';
    // Setup search listener when modal is shown
    setTimeout(setupMemberSearch, 100);
}

async function leaveGroup(groupId) {
    if (!confirm('Are you sure you want to leave this group? You will no longer have access to it.')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/groups/${groupId}/leave`, {
            method: 'POST'
        });
        
        if (response.ok) {
            alert('You have left the group');
            loadGroups();
        } else {
            const data = await response.json();
            alert('Error: ' + (data.error || 'Failed to leave group'));
        }
    } catch (error) {
        console.error('Error leaving group:', error);
        alert('An error occurred. Please try again.');
    }
}

async function addMemberToGroup(userId, username, name) {
    const groupId = document.getElementById('add-member-group-id').value;
    
    try {
        const response = await fetch(`/api/groups/${encodeURIComponent(groupId)}/members`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ userId: parseInt(userId) })
        });
        
        if (response.ok) {
            alert(`${name} has been added to the group!`);
            closeAddMemberModal();
            loadGroups();
        } else {
            const data = await response.json();
            alert('Error: ' + (data.error || 'Failed to add member'));
        }
    } catch (error) {
        console.error('Error adding member:', error);
        alert('An error occurred. Please try again.');
    }
}

document.getElementById('join-group-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const groupId = document.getElementById('group-id').value.trim();
    
    if (!groupId) {
        alert('Please enter a group ID');
        return;
    }
    
    try {
        const response = await fetch(`/api/groups/${encodeURIComponent(groupId)}/join`, {
            method: 'POST'
        });
        
        if (response.ok) {
            alert('Successfully joined the group!');
            closeJoinGroupModal();
            loadGroups();
        } else {
            const data = await response.json();
            alert('Error: ' + (data.error || 'Failed to join group'));
        }
    } catch (error) {
        console.error('Error joining group:', error);
        alert('An error occurred. Please try again.');
    }
});


function copyGroupId(groupId) {
    const groupIdElement = document.getElementById(`group-id-${groupId}`);
    if (!groupIdElement) {
        console.error('Group ID element not found');
        return;
    }
    const groupIdText = groupIdElement.textContent;
    
    // Find the button associated with this groupId
    const button = groupIdElement.parentElement.querySelector('.btn-copy');
    
    navigator.clipboard.writeText(groupIdText).then(() => {
        // Show feedback
        if (button) {
            const originalText = button.textContent;
            button.textContent = '✓';
            button.style.color = '#28a745';
            setTimeout(() => {
                button.textContent = originalText;
                button.style.color = '';
            }, 2000);
        }
    }).catch(err => {
        console.error('Failed to copy:', err);
        // Fallback for older browsers
        const textArea = document.createElement('textarea');
        textArea.value = groupIdText;
        document.body.appendChild(textArea);
        textArea.select();
        try {
            document.execCommand('copy');
            alert('Group ID copied to clipboard!');
        } catch (fallbackErr) {
            alert('Failed to copy. Please copy manually: ' + groupIdText);
        }
        document.body.removeChild(textArea);
    });
}

window.showEditDescriptionModal = function(groupId, groupName, description) {
    console.log('showEditDescriptionModal called with:', groupId, groupName, description);
    const modal = document.getElementById('edit-description-modal');
    if (!modal) {
        console.error('Edit description modal not found');
        alert('Edit description modal not found. Please refresh the page.');
        return;
    }
    modal.classList.add('active');
    modal.style.display = 'flex';
    const nameInput = document.getElementById('edit-group-name');
    const descInput = document.getElementById('edit-description-text');
    const descCount = document.getElementById('edit-description-count');
    document.getElementById('edit-description-group-id').value = groupId;
    nameInput.value = groupName || '';
    descInput.value = description || '';
    if (descCount) {
        descCount.textContent = `${descInput.value.length}/36 characters`;
    }
};

function closeEditDescriptionModal() {
    const modal = document.getElementById('edit-description-modal');
    modal.classList.remove('active');
    modal.style.display = 'none';
    document.getElementById('edit-description-form').reset();
}

document.getElementById('edit-description-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const groupId = document.getElementById('edit-description-group-id').value;
    const name = document.getElementById('edit-group-name').value;
    const description = document.getElementById('edit-description-text').value;
    
    try {
        const response = await fetch(`/api/groups/${encodeURIComponent(groupId)}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ name: name, description: description })
        });
        
        if (response.ok) {
            closeEditDescriptionModal();
            loadGroups();
        } else {
            const data = await response.json();
            alert('Error: ' + (data.error || 'Failed to update group'));
        }
    } catch (error) {
        console.error('Error updating group:', error);
        alert('An error occurred. Please try again.');
    }
});

// Close modals when clicking outside
window.onclick = function(event) {
    const createModal = document.getElementById('create-group-modal');
    const joinModal = document.getElementById('join-group-modal');
    const addMemberModal = document.getElementById('add-member-modal');
    const editDescriptionModal = document.getElementById('edit-description-modal');
    
    if (event.target === createModal) {
        closeCreateGroupModal();
    }
    if (event.target === joinModal) {
        closeJoinGroupModal();
    }
    if (event.target === addMemberModal) {
        closeAddMemberModal();
    }
    if (event.target === editDescriptionModal) {
        closeEditDescriptionModal();
    }
}

// Character count for description fields
function setupDescriptionCounters() {
    const createDesc = document.getElementById('group-description');
    const createCount = document.getElementById('group-description-count');
    const editDesc = document.getElementById('edit-description-text');
    const editCount = document.getElementById('edit-description-count');

    if (createDesc && createCount) {
        createDesc.addEventListener('input', function() {
            createCount.textContent = `${this.value.length}/36 characters`;
        });
    }

    if (editDesc && editCount) {
        editDesc.addEventListener('input', function() {
            editCount.textContent = `${this.value.length}/36 characters`;
        });
        
        // Update count when modal is opened
        const originalShow = window.showEditDescriptionModal;
        window.showEditDescriptionModal = function(groupId, groupName, description) {
            originalShow(groupId, groupName, description);
            if (editDesc && editCount) {
                editCount.textContent = `${editDesc.value.length}/36 characters`;
            }
        };
    }
}

// Load groups on page load
document.addEventListener('DOMContentLoaded', async () => {
    await loadCurrentUser();
    setupDescriptionCounters();
    loadGroups();
});

