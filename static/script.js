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
        
        if (query.length < 2) {
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
                e.preventDefault();
                if (selectedIndex >= 0 && currentSuggestions[selectedIndex]) {
                    selectCity(currentSuggestions[selectedIndex]);
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
        dropdown.innerHTML = '<div class="autocomplete-loading">🔍 Searching cities...</div>';
        
        // Get fuzzy match results first (always available)
        const fuzzyResults = getFallbackCities(query);
        
        // Try to get API results
        let apiResults = [];
        try {
            const response = await fetch(`/api/cities?q=${encodeURIComponent(query)}`);
            if (response.ok) {
                apiResults = await response.json();
            }
        } catch (apiError) {
            console.log('API request failed, using fuzzy matching only');
        }
        
        // Combine and deduplicate results, prioritizing fuzzy matches for relevance
        const combinedResults = combineAndRankResults(apiResults, fuzzyResults, query);
        
        currentSuggestions = combinedResults;
        selectedIndex = -1;
        
        displaySuggestions(combinedResults);
        
    } catch (error) {
        console.error('Error in city suggestions:', error);
        
        // Final fallback: just use fuzzy matching
        const fallbackCities = getFallbackCities(query);
        currentSuggestions = fallbackCities;
        selectedIndex = -1;
        displaySuggestions(fallbackCities);
    }
}

function combineAndRankResults(apiResults, fuzzyResults, query) {
    // Create a map to track cities we've seen to avoid duplicates
    const seenCities = new Map();
    const allResults = [];
    
    // Add fuzzy results first (they have better matching logic)
    fuzzyResults.forEach(city => {
        const key = `${city.name.toLowerCase()}-${city.country.toLowerCase()}`;
        if (!seenCities.has(key)) {
            seenCities.set(key, true);
            allResults.push({
                ...city,
                source: 'fuzzy',
                relevanceScore: calculateRelevanceScore(city.name, query)
            });
        }
    });
    
    // Add API results, but only if they're not already included and seem relevant
    apiResults.forEach(city => {
        const key = `${city.name.toLowerCase()}-${city.country.toLowerCase()}`;
        if (!seenCities.has(key)) {
            const relevanceScore = calculateRelevanceScore(city.name, query);
            // Use the same strict threshold as fuzzy matching - only genuinely relevant results
            if (relevanceScore > 450) {
                seenCities.set(key, true);
                allResults.push({
                    ...city,
                    source: 'api',
                    relevanceScore: relevanceScore
                });
            }
        }
    });
    
    // Sort by relevance score (highest first) and return top 8
    return allResults
        .sort((a, b) => b.relevanceScore - a.relevanceScore)
        .slice(0, 8)
        .map(city => ({
            name: city.name,
            country: city.country,
            display: city.display,
            searchValue: city.searchValue || city.name
        }));
}

function calculateRelevanceScore(cityName, query) {
    const city_lower = cityName.toLowerCase();
    const query_lower = query.toLowerCase().trim();
    
    // Exact match (highest priority)
    if (city_lower === query_lower) return 1000;
    
    // Starts with (very high priority)
    if (city_lower.startsWith(query_lower)) return 900;
    
    // Contains query (high priority)
    if (city_lower.includes(query_lower)) return 800;
    
    // Only do expensive fuzzy matching for queries of 3+ characters
    if (query_lower.length < 3) return 0;
    
    // Levenshtein distance with strict threshold
    const editDistance = levenshteinDistance(city_lower, query_lower);
    const maxAllowedDistance = Math.max(1, Math.floor(query_lower.length * 0.4)); // Allow max 40% character changes
    
    if (editDistance <= maxAllowedDistance) {
        const levenshteinScore = 700 - (editDistance * 100);
        if (levenshteinScore > 500) return levenshteinScore;
    }
    
    // Jaro-Winkler with strict threshold
    const jaroSimilarity = jaroWinklerSimilarity(city_lower, query_lower);
    if (jaroSimilarity > 0.7) {
        return jaroSimilarity * 600;
    }
    
    // N-gram with strict threshold
    const ngramSimilarity2 = ngramSimilarity(city_lower, query_lower);
    if (ngramSimilarity2 > 0.5) {
        return ngramSimilarity2 * 500;
    }
    
    return 0; // No match
}

function getFallbackCities(query) {
    // Compact list of major cities - no aliases needed!
    const majorCities = [
        { name: 'New York', country: 'US', display: 'New York, United States' },
        { name: 'Los Angeles', country: 'US', display: 'Los Angeles, United States' },
        { name: 'Chicago', country: 'US', display: 'Chicago, United States' },
        { name: 'Houston', country: 'US', display: 'Houston, United States' },
        { name: 'Phoenix', country: 'US', display: 'Phoenix, United States' },
        { name: 'Philadelphia', country: 'US', display: 'Philadelphia, United States' },
        { name: 'San Antonio', country: 'US', display: 'San Antonio, United States' },
        { name: 'San Diego', country: 'US', display: 'San Diego, United States' },
        { name: 'Dallas', country: 'US', display: 'Dallas, United States' },
        { name: 'Austin', country: 'US', display: 'Austin, United States' },
        { name: 'Jacksonville', country: 'US', display: 'Jacksonville, United States' },
        { name: 'Fort Worth', country: 'US', display: 'Fort Worth, United States' },
        { name: 'Columbus', country: 'US', display: 'Columbus, United States' },
        { name: 'Indianapolis', country: 'US', display: 'Indianapolis, United States' },
        { name: 'Charlotte', country: 'US', display: 'Charlotte, United States' },
        { name: 'San Francisco', country: 'US', display: 'San Francisco, United States' },
        { name: 'Seattle', country: 'US', display: 'Seattle, United States' },
        { name: 'Denver', country: 'US', display: 'Denver, United States' },
        { name: 'Washington', country: 'US', display: 'Washington, United States' },
        { name: 'Boston', country: 'US', display: 'Boston, United States' },
        { name: 'Nashville', country: 'US', display: 'Nashville, United States' },
        { name: 'Baltimore', country: 'US', display: 'Baltimore, United States' },
        { name: 'Louisville', country: 'US', display: 'Louisville, United States' },
        { name: 'Portland', country: 'US', display: 'Portland, United States' },
        { name: 'Oklahoma City', country: 'US', display: 'Oklahoma City, United States' },
        { name: 'Milwaukee', country: 'US', display: 'Milwaukee, United States' },
        { name: 'Las Vegas', country: 'US', display: 'Las Vegas, United States' },
        { name: 'Albuquerque', country: 'US', display: 'Albuquerque, United States' },
        { name: 'Tucson', country: 'US', display: 'Tucson, United States' },
        { name: 'Fresno', country: 'US', display: 'Fresno, United States' },
        { name: 'Sacramento', country: 'US', display: 'Sacramento, United States' },
        { name: 'Mesa', country: 'US', display: 'Mesa, United States' },
        { name: 'Kansas City', country: 'US', display: 'Kansas City, United States' },
        { name: 'Atlanta', country: 'US', display: 'Atlanta, United States' },
        { name: 'Colorado Springs', country: 'US', display: 'Colorado Springs, United States' },
        { name: 'Raleigh', country: 'US', display: 'Raleigh, United States' },
        { name: 'Omaha', country: 'US', display: 'Omaha, United States' },
        { name: 'Miami', country: 'US', display: 'Miami, United States' },
        { name: 'Minneapolis', country: 'US', display: 'Minneapolis, United States' },
        { name: 'Tampa', country: 'US', display: 'Tampa, United States' },
        { name: 'New Orleans', country: 'US', display: 'New Orleans, United States' },
        { name: 'Cleveland', country: 'US', display: 'Cleveland, United States' },
        { name: 'Honolulu', country: 'US', display: 'Honolulu, United States' },
        { name: 'Cincinnati', country: 'US', display: 'Cincinnati, United States' },
        { name: 'Pittsburgh', country: 'US', display: 'Pittsburgh, United States' },
        { name: 'Orlando', country: 'US', display: 'Orlando, United States' },
        { name: 'Buffalo', country: 'US', display: 'Buffalo, United States' },
        
        // Major International Cities
        { name: 'London', country: 'GB', display: 'London, United Kingdom' },
        { name: 'Paris', country: 'FR', display: 'Paris, France' },
        { name: 'Tokyo', country: 'JP', display: 'Tokyo, Japan' },
        { name: 'Berlin', country: 'DE', display: 'Berlin, Germany' },
        { name: 'Madrid', country: 'ES', display: 'Madrid, Spain' },
        { name: 'Rome', country: 'IT', display: 'Rome, Italy' },
        { name: 'Moscow', country: 'RU', display: 'Moscow, Russia' },
        { name: 'Beijing', country: 'CN', display: 'Beijing, China' },
        { name: 'Mumbai', country: 'IN', display: 'Mumbai, India' },
        { name: 'São Paulo', country: 'BR', display: 'São Paulo, Brazil' },
        { name: 'Mexico City', country: 'MX', display: 'Mexico City, Mexico' },
        { name: 'Istanbul', country: 'TR', display: 'Istanbul, Turkey' },
        { name: 'Lagos', country: 'NG', display: 'Lagos, Nigeria' },
        { name: 'Cairo', country: 'EG', display: 'Cairo, Egypt' },
        { name: 'Shanghai', country: 'CN', display: 'Shanghai, China' },
        { name: 'Buenos Aires', country: 'AR', display: 'Buenos Aires, Argentina' },
        { name: 'Jakarta', country: 'ID', display: 'Jakarta, Indonesia' },
        { name: 'Manila', country: 'PH', display: 'Manila, Philippines' },
        { name: 'Seoul', country: 'KR', display: 'Seoul, South Korea' },
        { name: 'Bangkok', country: 'TH', display: 'Bangkok, Thailand' },
        { name: 'Sydney', country: 'AU', display: 'Sydney, Australia' },
        { name: 'Melbourne', country: 'AU', display: 'Melbourne, Australia' },
        { name: 'Toronto', country: 'CA', display: 'Toronto, Canada' },
        { name: 'Vancouver', country: 'CA', display: 'Vancouver, Canada' },
        { name: 'Montreal', country: 'CA', display: 'Montreal, Canada' },
        { name: 'Amsterdam', country: 'NL', display: 'Amsterdam, Netherlands' },
        { name: 'Barcelona', country: 'ES', display: 'Barcelona, Spain' },
        { name: 'Munich', country: 'DE', display: 'Munich, Germany' },
        { name: 'Milan', country: 'IT', display: 'Milan, Italy' },
        { name: 'Stockholm', country: 'SE', display: 'Stockholm, Sweden' },
        { name: 'Copenhagen', country: 'DK', display: 'Copenhagen, Denmark' },
        { name: 'Oslo', country: 'NO', display: 'Oslo, Norway' },
        { name: 'Helsinki', country: 'FI', display: 'Helsinki, Finland' },
        { name: 'Dublin', country: 'IE', display: 'Dublin, Ireland' },
        { name: 'Vienna', country: 'AT', display: 'Vienna, Austria' },
        { name: 'Zurich', country: 'CH', display: 'Zurich, Switzerland' },
        { name: 'Brussels', country: 'BE', display: 'Brussels, Belgium' },
        { name: 'Prague', country: 'CZ', display: 'Prague, Czech Republic' },
        { name: 'Warsaw', country: 'PL', display: 'Warsaw, Poland' }
    ];
    
    const query_lower = query.toLowerCase().trim();
    
    // Calculate fuzzy match scores using multiple algorithms
    function calculateFuzzyScore(cityName, query) {
        const city_lower = cityName.toLowerCase();
        
        // 1. Exact match (highest priority)
        if (city_lower === query_lower) return 1000;
        
        // 2. Starts with (very high priority)
        if (city_lower.startsWith(query_lower)) return 900;
        
        // 3. Contains query (high priority)
        if (city_lower.includes(query_lower)) return 800;
        
        // Only do expensive fuzzy matching for queries of 3+ characters
        if (query_lower.length < 3) return 0;
        
        // 4. Levenshtein distance (edit distance) - be more strict
        const editDistance = levenshteinDistance(city_lower, query_lower);
        const maxAllowedDistance = Math.max(1, Math.floor(query_lower.length * 0.4)); // Allow max 40% character changes
        
        if (editDistance <= maxAllowedDistance) {
            const levenshteinScore = 700 - (editDistance * 100);
            if (levenshteinScore > 500) return levenshteinScore; // Only return if it's a really good match
        }
        
        // 5. Jaro-Winkler similarity (fuzzy matching) - be more strict
        const jaroSimilarity = jaroWinklerSimilarity(city_lower, query_lower);
        if (jaroSimilarity > 0.7) { // Only if similarity is > 70%
            return jaroSimilarity * 600;
        }
        
        // 6. N-gram similarity (partial matching) - be more strict
        const ngramSimilarity2 = ngramSimilarity(city_lower, query_lower);
        if (ngramSimilarity2 > 0.5) { // Only if similarity is > 50%
            return ngramSimilarity2 * 500;
        }
        
        return 0; // No match if none of the above criteria are met
    }
    
    // Score and filter cities with stricter threshold
    const scoredCities = majorCities
        .map(city => ({
            ...city,
            score: calculateFuzzyScore(city.name, query)
        }))
        .filter(city => city.score > 450) // Much stricter threshold - only keep very good matches
        .sort((a, b) => b.score - a.score)
        .slice(0, 8)
        .map(city => ({
            name: city.name,
            country: city.country,
            display: city.display,
            searchValue: city.name
        }));
    
    return scoredCities;
}

// Levenshtein Distance Algorithm (edit distance)
function levenshteinDistance(str1, str2) {
    const matrix = Array(str2.length + 1).fill(null).map(() => Array(str1.length + 1).fill(null));
    
    for (let i = 0; i <= str1.length; i++) matrix[0][i] = i;
    for (let j = 0; j <= str2.length; j++) matrix[j][0] = j;
    
    for (let j = 1; j <= str2.length; j++) {
        for (let i = 1; i <= str1.length; i++) {
            const substitutionCost = str1[i - 1] === str2[j - 1] ? 0 : 1;
            matrix[j][i] = Math.min(
                matrix[j][i - 1] + 1,     // deletion
                matrix[j - 1][i] + 1,     // insertion
                matrix[j - 1][i - 1] + substitutionCost // substitution
            );
        }
    }
    
    return matrix[str2.length][str1.length];
}

// Jaro-Winkler Similarity (fuzzy string matching)
function jaroWinklerSimilarity(str1, str2) {
    if (str1 === str2) return 1;
    if (str1.length === 0 || str2.length === 0) return 0;
    
    const matchWindow = Math.floor(Math.max(str1.length, str2.length) / 2) - 1;
    const str1Matches = new Array(str1.length).fill(false);
    const str2Matches = new Array(str2.length).fill(false);
    
    let matches = 0;
    let transpositions = 0;
    
    // Find matches
    for (let i = 0; i < str1.length; i++) {
        const start = Math.max(0, i - matchWindow);
        const end = Math.min(i + matchWindow + 1, str2.length);
        
        for (let j = start; j < end; j++) {
            if (str2Matches[j] || str1[i] !== str2[j]) continue;
            str1Matches[i] = true;
            str2Matches[j] = true;
            matches++;
            break;
        }
    }
    
    if (matches === 0) return 0;
    
    // Find transpositions
    let k = 0;
    for (let i = 0; i < str1.length; i++) {
        if (!str1Matches[i]) continue;
        while (!str2Matches[k]) k++;
        if (str1[i] !== str2[k]) transpositions++;
        k++;
    }
    
    const jaro = (matches / str1.length + matches / str2.length + 
                  (matches - transpositions / 2) / matches) / 3;
    
    // Winkler prefix scaling
    const prefix = Math.min(4, commonPrefixLength(str1, str2));
    return jaro + (0.1 * prefix * (1 - jaro));
}

// N-gram Similarity (substring matching)
function ngramSimilarity(str1, str2, n = 2) {
    if (str1.length < n || str2.length < n) return 0;
    
    const ngrams1 = new Set();
    const ngrams2 = new Set();
    
    for (let i = 0; i <= str1.length - n; i++) {
        ngrams1.add(str1.substr(i, n));
    }
    
    for (let i = 0; i <= str2.length - n; i++) {
        ngrams2.add(str2.substr(i, n));
    }
    
    const intersection = new Set([...ngrams1].filter(x => ngrams2.has(x)));
    const union = new Set([...ngrams1, ...ngrams2]);
    
    return intersection.size / union.size;
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
    
    // Optional: automatically submit the form
    // document.querySelector('.search-form').submit();
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
