// Track current toggle states
let currentTimezone = 'your'; // 'your' or 'local'
let currentUnits = 'metric'; // 'metric' or 'imperial'
let currentTimeFormat = '24hour'; // '24hour' or '12hour'

function toggleTimezone(showLocal) {
    const localElements = document.querySelectorAll('.local-time');
    const yourElements = document.querySelectorAll('.your-time');
    const localBtn = document.getElementById('local-btn');
    const yourBtn = document.getElementById('your-btn');
    
    currentTimezone = showLocal ? 'local' : 'your';
    updateFormFields();
    
    if (showLocal) {
        localElements.forEach(el => el.style.display = 'inline');
        yourElements.forEach(el => el.style.display = 'none');
        localBtn.classList.add('active');
        yourBtn.classList.remove('active');
    } else {
        localElements.forEach(el => el.style.display = 'none');
        yourElements.forEach(el => el.style.display = 'inline');
        yourBtn.classList.add('active');
        localBtn.classList.remove('active');
    }
}

function toggleUnits(useImperial) {
    const metricElements = document.querySelectorAll('.metric-units');
    const imperialElements = document.querySelectorAll('.imperial-units');
    const metricBtn = document.getElementById('metric-btn');
    const imperialBtn = document.getElementById('imperial-btn');
    
    currentUnits = useImperial ? 'imperial' : 'metric';
    updateFormFields();
    
    if (useImperial) {
        imperialElements.forEach(el => el.style.display = 'inline');
        metricElements.forEach(el => el.style.display = 'none');
        imperialBtn.classList.add('active');
        metricBtn.classList.remove('active');
    } else {
        metricElements.forEach(el => el.style.display = 'inline');
        imperialElements.forEach(el => el.style.display = 'none');
        metricBtn.classList.add('active');
        imperialBtn.classList.remove('active');
    }
}

function toggleTimeFormat(use12Hour) {
    const time24Elements = document.querySelectorAll('.time-24h');
    const time12Elements = document.querySelectorAll('.time-12h');
    const time24Btn = document.getElementById('time24-btn');
    const time12Btn = document.getElementById('time12-btn');
    
    currentTimeFormat = use12Hour ? '12hour' : '24hour';
    updateFormFields();
    
    if (use12Hour) {
        time12Elements.forEach(el => el.style.display = 'inline');
        time24Elements.forEach(el => el.style.display = 'none');
        time12Btn.classList.add('active');
        time24Btn.classList.remove('active');
    } else {
        time24Elements.forEach(el => el.style.display = 'inline');
        time12Elements.forEach(el => el.style.display = 'none');
        time24Btn.classList.add('active');
        time12Btn.classList.remove('active');
    }
}

function updateFormFields() {
    // Update hidden form fields with current toggle states
    const timezoneField = document.getElementById('timezone-field');
    const unitsField = document.getElementById('units-field');
    const timeFormatField = document.getElementById('timeformat-field');
    
    if (timezoneField) timezoneField.value = currentTimezone;
    if (unitsField) unitsField.value = currentUnits;
    if (timeFormatField) timeFormatField.value = currentTimeFormat;
}

// Initialize toggle states when page loads
document.addEventListener('DOMContentLoaded', function() {
    // Read initial states from form fields
    const timezoneField = document.getElementById('timezone-field');
    const unitsField = document.getElementById('units-field');
    const timeFormatField = document.getElementById('timeformat-field');
    
    currentTimezone = timezoneField ? timezoneField.value : 'your';
    currentUnits = unitsField ? unitsField.value : 'metric';
    currentTimeFormat = timeFormatField ? timeFormatField.value : '24hour';
    
    // Set button states based on current values
    if (currentTimezone === 'local') {
        toggleTimezone(true);
    }
    if (currentUnits === 'imperial') {
        toggleUnits(true);
    }
    if (currentTimeFormat === '12hour') {
        toggleTimeFormat(true);
    }
    
    updateFormFields();
});

// Clear input function
function clearInput() {
    const cityInput = document.getElementById('city-input');
    cityInput.value = '';
    cityInput.focus();
    toggleClearButton();
}

// Show/hide clear button based on input content
function toggleClearButton() {
    const cityInput = document.getElementById('city-input');
    const clearButton = document.querySelector('.clear-button');
    
    if (cityInput.value.trim() === '') {
        clearButton.style.opacity = '0';
        clearButton.style.pointerEvents = 'none';
    } else {
        clearButton.style.opacity = '1';
        clearButton.style.pointerEvents = 'auto';
    }
}

// Add event listener for input changes when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    const cityInput = document.getElementById('city-input');
    if (cityInput) {
        cityInput.addEventListener('input', toggleClearButton);
        // Initialize clear button visibility
        toggleClearButton();
    }
});
