// Theme Toggle Functionality
(function() {
    // Get theme from localStorage or default to light
    const getTheme = () => {
        return localStorage.getItem('theme') || 'light';
    };

    // Set theme
    const setTheme = (theme) => {
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem('theme', theme);
        updateToggleIcon(theme);
    };

    // Update toggle icon
    const updateToggleIcon = (theme) => {
        const toggle = document.getElementById('theme-toggle');
        if (toggle) {
            toggle.textContent = theme === 'dark' ? '☀️' : '🌙';
            toggle.setAttribute('aria-label', theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode');
        }
    };

    // Initialize theme IMMEDIATELY to prevent flash (before DOMContentLoaded)
    (function() {
        const currentTheme = getTheme();
        document.documentElement.setAttribute('data-theme', currentTheme);
    })();

    // Update toggle icon after DOM loads
    document.addEventListener('DOMContentLoaded', () => {
        const currentTheme = getTheme();
        updateToggleIcon(currentTheme);
    });

    // Toggle theme function (global)
    window.toggleTheme = function() {
        const currentTheme = getTheme();
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
        setTheme(newTheme);
    };
})();

