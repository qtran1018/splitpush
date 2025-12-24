const registerForm = document.getElementById('register-form');
if (registerForm) {
    protectFormSubmission(registerForm, async (e) => {
        const formData = {
            username: document.getElementById('username').value,
            email: document.getElementById('email').value,
            name: document.getElementById('name').value,
            password: document.getElementById('password').value
        };

        const errorDiv = document.getElementById('error-message');
        const successDiv = document.getElementById('success-message');
        
        errorDiv.style.display = 'none';
        successDiv.style.display = 'none';

        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });

        const data = await response.json();

        if (response.ok) {
            successDiv.textContent = 'Registration successful! Redirecting to login...';
            successDiv.style.display = 'block';
            setTimeout(() => {
                window.location.href = '/login';
            }, 2000);
        } else {
            errorDiv.textContent = data.error || 'Registration failed';
            errorDiv.style.display = 'block';
            throw new Error('Registration failed'); // Re-enable form on error
        }
    }, {
        loadingText: 'Registering...',
        submitButtonSelector: 'button[type="submit"]'
    });
}

