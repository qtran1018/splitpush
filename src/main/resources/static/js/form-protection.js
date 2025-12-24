/**
 * Form Protection Utility
 * Prevents duplicate submissions by disabling buttons and tracking request state
 */

/**
 * Protects a form submission from duplicate clicks
 * @param {HTMLFormElement} form - The form element
 * @param {Function} submitHandler - The async function that handles the form submission
 * @param {Object} options - Configuration options
 * @param {string} options.submitButtonSelector - Selector for the submit button (default: 'button[type="submit"]')
 * @param {string} options.loadingText - Text to show while loading (default: 'Saving...')
 * @param {boolean} options.disableAllButtons - Whether to disable all buttons in the form (default: true)
 */
function protectFormSubmission(form, submitHandler, options = {}) {
    const {
        submitButtonSelector = 'button[type="submit"]',
        loadingText = 'Saving...',
        disableAllButtons = true
    } = options;

    let isSubmitting = false;
    const submitButton = form.querySelector(submitButtonSelector);
    const originalButtonText = submitButton ? submitButton.textContent : '';

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        // Prevent multiple submissions
        if (isSubmitting) {
            console.warn('Form submission already in progress, ignoring duplicate click');
            return;
        }

        // Disable form and show loading state
        isSubmitting = true;
        const allButtons = disableAllButtons ? form.querySelectorAll('button') : [submitButton].filter(Boolean);
        
        allButtons.forEach(btn => {
            if (btn) {
                btn.disabled = true;
                btn.style.opacity = '0.6';
                btn.style.cursor = 'not-allowed';
            }
        });

        if (submitButton) {
            submitButton.textContent = loadingText;
        }

        try {
            await submitHandler(e);
        } catch (error) {
            console.error('Form submission error:', error);
            // Error handling is done in the submitHandler
        } finally {
            // Re-enable form after a short delay to ensure request completes
            setTimeout(() => {
                isSubmitting = false;
                allButtons.forEach(btn => {
                    if (btn) {
                        btn.disabled = false;
                        btn.style.opacity = '';
                        btn.style.cursor = '';
                    }
                });
                if (submitButton) {
                    submitButton.textContent = originalButtonText;
                }
            }, 500);
        }
    });
}

/**
 * Protects a button click from duplicate submissions
 * @param {HTMLElement} button - The button element
 * @param {Function} clickHandler - The async function that handles the button click
 * @param {Object} options - Configuration options
 * @param {string} options.loadingText - Text to show while loading (default: 'Processing...')
 */
function protectButtonClick(button, clickHandler, options = {}) {
    const { loadingText = 'Processing...' } = options;
    let isProcessing = false;
    const originalButtonText = button.textContent;

    button.addEventListener('click', async (e) => {
        e.preventDefault();
        e.stopPropagation();

        // Prevent multiple clicks
        if (isProcessing) {
            console.warn('Button action already in progress, ignoring duplicate click');
            return;
        }

        // Disable button and show loading state
        isProcessing = true;
        button.disabled = true;
        button.style.opacity = '0.6';
        button.style.cursor = 'not-allowed';
        button.textContent = loadingText;

        try {
            await clickHandler(e);
        } catch (error) {
            console.error('Button action error:', error);
            // Error handling is done in the clickHandler
        } finally {
            // Re-enable button after a short delay
            setTimeout(() => {
                isProcessing = false;
                button.disabled = false;
                button.style.opacity = '';
                button.style.cursor = '';
                button.textContent = originalButtonText;
            }, 500);
        }
    });
}

