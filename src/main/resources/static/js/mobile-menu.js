// Mobile menu toggle functionality
window.toggleMobileMenu = function() {
    const navLinks = document.getElementById('nav-links');
    if (navLinks) {
        navLinks.classList.toggle('active');
    }
};

// Close mobile menu when clicking outside
document.addEventListener('click', function(event) {
    const navLinks = document.getElementById('nav-links');
    const hamburger = document.querySelector('.hamburger-menu');
    
    if (navLinks && hamburger && 
        !navLinks.contains(event.target) && 
        !hamburger.contains(event.target)) {
        navLinks.classList.remove('active');
    }
});

// Close mobile menu when clicking a nav link
document.addEventListener('DOMContentLoaded', function() {
    const navLinks = document.getElementById('nav-links');
    if (navLinks) {
        const links = navLinks.querySelectorAll('.nav-link');
        links.forEach(link => {
            link.addEventListener('click', function() {
                navLinks.classList.remove('active');
            });
        });
    }
});

