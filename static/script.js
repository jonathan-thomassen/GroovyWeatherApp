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
    hideAutocomplete();
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

// Autocomplete functionality
let autocompleteTimeout;
let currentSuggestions = [];
let selectedIndex = -1;

function setupAutocomplete() {
    const cityInput = document.getElementById('city-input');
    const dropdown = document.getElementById('city-dropdown');
    
    if (!cityInput || !dropdown) return;
    
    // Input event listener with debouncing
    cityInput.addEventListener('input', function(e) {
        const query = e.target.value.trim();
        
        clearTimeout(autocompleteTimeout);
        
        if (query.length < 3) {
            hideAutocomplete();
            return;
        }
        
        // Debounce API calls by 300ms
        autocompleteTimeout = setTimeout(() => {
            fetchCitySuggestions(query);
        }, 300);
    });
    
    // Keyboard navigation
    cityInput.addEventListener('keydown', function(e) {
        const dropdown = document.getElementById('city-dropdown');
        if (!dropdown.classList.contains('show')) return;
        
        switch(e.key) {
            case 'ArrowDown':
                e.preventDefault();
                selectedIndex = Math.min(selectedIndex + 1, currentSuggestions.length - 1);
                updateSelection();
                break;
            case 'ArrowUp':
                e.preventDefault();
                selectedIndex = Math.max(selectedIndex - 1, -1);
                updateSelection();
                break;
            case 'Enter':
                if (selectedIndex >= 0 && currentSuggestions[selectedIndex]) {
                    e.preventDefault();
                    selectCity(currentSuggestions[selectedIndex]);
                } else {
                    // Allow form submission when no suggestion is selected
                    hideAutocomplete();
                }
                break;
            case 'Escape':
                hideAutocomplete();
                break;
        }
    });
    
    // Click outside to close dropdown
    document.addEventListener('click', function(e) {
        if (!cityInput.contains(e.target) && !dropdown.contains(e.target)) {
            hideAutocomplete();
        }
    });
}

async function fetchCitySuggestions(query) {
    const dropdown = document.getElementById('city-dropdown');
    
    try {
        // Show loading state
        showAutocomplete();
        dropdown.innerHTML = '<div class="autocomplete-loading">Searching cities...</div>';
        
        // Try to get API results with progressive query refinement
        const apiResults = await searchCitiesWithRetry(query);
        
        // If API returns good results, use them
        if (apiResults.length > 0) {
            currentSuggestions = apiResults;
            selectedIndex = -1;
            displaySuggestions(apiResults);
            return;
        }
        
    } catch (error) {
        console.error('Error in city suggestions:', error);
    }
}

// Cache for API results to reduce redundant calls
const cityCache = new Map();
const CACHE_DURATION = 300000; // 5 minutes

async function searchCitiesWithRetry(query) {
    // Check cache first
    const cacheKey = query.toLowerCase().trim();
    const cached = cityCache.get(cacheKey);
    if (cached && Date.now() - cached.timestamp < CACHE_DURATION) {
        return cached.results;
    }
    
    const searchVariations = generateSearchVariations(query);
    
    for (const searchQuery of searchVariations) {
        try {
            // Call our server endpoint which will proxy to Google Places API
            const response = await fetch(`/api/cities?q=${encodeURIComponent(searchQuery)}`);
            if (response.ok) {
                const results = await response.json();
                if (results.length > 0) {
                    // Cache the results
                    cityCache.set(cacheKey, {
                        results: results,
                        timestamp: Date.now()
                    });
                    return results;
                }
            }
        } catch (error) {
            console.log(`Search failed for variation: ${searchQuery}`, error);
            continue; // Try next variation
        }
    }
    
    return []; // No results found
}

function generateSearchVariations(query) {
    const variations = [query.trim()];
    const queryLower = query.toLowerCase().trim();
    
    // If query contains comma, try different formats
    if (queryLower.includes(',')) {
        const parts = queryLower.split(',').map(p => p.trim());
        if (parts.length >= 2) {
            // Try just the city name
            variations.push(parts[0]);
            // Try city + country if it looks like a country code
            if (parts[1].length === 2) {
                variations.push(`${parts[0]},${parts[1].toUpperCase()}`);
            }
        }
    } else if (queryLower.includes(' ')) {
        // For space-separated queries, try different combinations
        const parts = queryLower.split(' ').filter(p => p.length > 0);
        if (parts.length >= 2) {
            // Try just the first part (likely city name)
            variations.push(parts[0]);
            // Try first part + last part (city + state/country)
            variations.push(`${parts[0]},${parts[parts.length - 1]}`);
        }
    }
    
    // Remove duplicates and return
    return [...new Set(variations)];
}

// Helper function for common prefix length
function commonPrefixLength(str1, str2) {
    let length = 0;
    const minLength = Math.min(str1.length, str2.length);
    
    for (let i = 0; i < minLength; i++) {
        if (str1[i] === str2[i]) {
            length++;
        } else {
            break;
        }
    }
    
    return length;
}

function displaySuggestions(cities) {
    const dropdown = document.getElementById('city-dropdown');
    
    if (!cities || cities.length === 0) {
        dropdown.innerHTML = '<div class="autocomplete-no-results">No cities found. Try a different search term.</div>';
        return;
    }
    
    const html = cities.map((city, index) => `
        <div class="autocomplete-item" data-index="${index}" onclick="selectCityByIndex(${index})">
            <span class="city-name">${city.display || `${city.name}, ${city.country}`}</span>
            <span class="country-info">${city.country}</span>
        </div>
    `).join('');
    
    dropdown.innerHTML = html;
}

function updateSelection() {
    const items = document.querySelectorAll('.autocomplete-item');
    items.forEach((item, index) => {
        item.classList.toggle('selected', index === selectedIndex);
    });
    
    // Scroll selected item into view
    if (selectedIndex >= 0 && items[selectedIndex]) {
        items[selectedIndex].scrollIntoView({ block: 'nearest' });
    }
}

function selectCityByIndex(index) {
    if (currentSuggestions[index]) {
        selectCity(currentSuggestions[index]);
    }
}

function selectCity(city) {
    const cityInput = document.getElementById('city-input');
    cityInput.value = city.searchValue || city.name || city.display;
    hideAutocomplete();
    toggleClearButton();
    
    // Automatically submit the form when a city is selected
    const form = document.querySelector('.search-form') || document.querySelector('form');
    if (form) {
        form.submit();
    }
}

function showAutocomplete() {
    const dropdown = document.getElementById('city-dropdown');
    dropdown.classList.add('show');
}

function hideAutocomplete() {
    const dropdown = document.getElementById('city-dropdown');
    dropdown.classList.remove('show');
    currentSuggestions = [];
    selectedIndex = -1;
}

// Add event listener for input changes when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    const cityInput = document.getElementById('city-input');
    if (cityInput) {
        cityInput.addEventListener('input', toggleClearButton);
        // Initialize clear button visibility
        toggleClearButton();
        
        // Setup autocomplete
        setupAutocomplete();
    }
});
