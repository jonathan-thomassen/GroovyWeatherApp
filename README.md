# GroovyWeatherApp

A modern, intelligent weather application built with Groovy and Jetty, featuring smart auto-completion, beautiful UI with animated backgrounds, comprehensive error handling, and responsive design.

## 🚀 Features

### Core Weather Features
- **Real-time Weather Data**: Current weather and 5-day forecast from OpenWeatherMap
- **Dual Timezone Support**: View weather in your timezone or local city time
- **Unit Conversion**: Toggle between Metric (°C) and Imperial (°F) units
- **Time Format Options**: 24-hour or 12-hour time display

### Smart User Interface
- **Intelligent Auto-Completion**: Advanced fuzzy matching with multiple algorithms
  - Levenshtein distance for typo tolerance
  - Jaro-Winkler similarity for partial matches
  - N-gram analysis for substring recognition
  - Real-time API integration with OpenWeatherMap geocoding
  - One-click form submission from suggestions
- **Beautiful Animated Header**: Dynamic gradient background with color transitions
- **Enhanced Input Field**: Clear button (×) with smart visibility
- **Modern Card Design**: Professional weather display with three-column layout

### User Experience
- **Smart Error Handling**: Categorized error messages with contextual styling
- **Responsive Design**: Optimized for desktop and mobile devices
- **Keyboard Navigation**: Full arrow key and Enter support in auto-complete
- **Loading States**: Visual feedback during API calls
- **Professional Typography**: Carefully crafted fonts and spacing

## 📁 Project Structure

```
GroovyWeatherApp/
├── src/
│   └── WeatherApp.groovy            # Consolidated application with all classes
├── static/
│   ├── index.html                   # Main HTML template
│   ├── styles.css                   # Complete CSS styling (900+ lines)
│   └── script.js                    # Advanced JavaScript with auto-completion
├── IsoCountryCodes.csv              # Country code to name mappings
├── key                              # OpenWeatherMap API key
└── README.md                        # This documentation
```

## 🛠️ Setup Instructions

### Prerequisites
- **Java 8 or higher**
- **Groovy 3.0 or higher**
- **OpenWeatherMap API key** (free account available)

### Installation

1. **Clone or download** the project to your local machine

2. **Get an API key** from [OpenWeatherMap](https://openweathermap.org/api)
   - Sign up for a free account
   - Generate an API key from your dashboard

3. **Create the API key file**:
   ```bash
   echo "your_api_key_here" > key
   ```
   *Replace `your_api_key_here` with your actual API key*

4. **Verify the CSV file exists**:
   - Ensure `IsoCountryCodes.csv` is in the root directory
   - This file maps ISO country codes to full country names

### Running the Application

```bash
cd src
groovy WeatherApp.groovy
```

The server will start on `http://localhost:8080`

You should see: `Server started at http://localhost:8080`

## 🎯 Using the Application

### Basic Usage
1. **Open your browser** to `http://localhost:8080`
2. **Start typing a city name** - intelligent suggestions will appear automatically
3. **Click any suggestion** - the form submits automatically and loads weather data
4. **Use toggle buttons** at the top to customize display options

### Auto-Completion Features
- **Type partial city names**: "Copen" → suggests "Copenhagen"
- **Handle typos**: "Berln" → suggests "Berlin"  
- **Use abbreviations**: "SF" → suggests "San Francisco"
- **Get instant results**: Click any suggestion to immediately load weather
- **Keyboard navigation**: Use ↑↓ arrows and Enter key

### Display Customization
- **🌍 Timezone Toggle**: Switch between your local time and city's local time
- **🌡️ Unit Toggle**: Switch between Celsius (°C) and Fahrenheit (°F)
- **🕐 Time Format**: Toggle between 24-hour (15:30) and 12-hour (3:30 PM) format

## 🔧 Advanced Configuration

### Server Port
To change the default port (8080), modify `WeatherApp.groovy`:
```groovy
def server = new Server(3000) // Change to your preferred port
```

### Auto-Complete Sensitivity
Adjust fuzzy matching thresholds in `script.js`:
```javascript
// Minimum relevance score for suggestions (higher = stricter)
if (relevanceScore > 450) { // Increase for fewer, more relevant results
```

## 🏗️ Architecture Overview

### Backend (Groovy)
- **WeatherApp.groovy**: Main server with embedded classes
  - `CountryCodeUtil`: ISO country code mapping
  - `TemperatureConverter`: Unified temperature operations
  - `WeatherFormatter`: HTML generation and error formatting
  - `WeatherApiClient`: HTTP client with proper resource management
  - `WeatherWebServer`: Jetty server and request handling

### Frontend (JavaScript/CSS)
- **Intelligent Auto-Completion**: Multi-algorithm fuzzy matching
- **Dynamic UI**: Animated gradients and smooth transitions
- **Responsive Design**: Mobile-first approach with breakpoints
- **Accessibility**: Keyboard navigation and screen reader support

### API Integration
- **Current Weather**: `api.openweathermap.org/data/2.5/weather`
- **5-Day Forecast**: `api.openweathermap.org/data/2.5/forecast`
- **Geocoding**: `api.openweathermap.org/geo/1.0/direct` (for auto-completion)

## 🎨 Key Improvements in New Structure

### Code Organization
- **Separated concerns**: Each class has a single responsibility
- **Modular design**: Easy to maintain and extend
- **Resource management**: Proper HTTP client cleanup prevents memory leaks
- **Consolidated utilities**: Temperature conversion logic unified

### Error Handling
- **Categorized errors**: Different error types with appropriate styling
- **Smart API logic**: Prevents duplicate error messages
- **Graceful degradation**: Shows partial results when possible

### Performance
- **Resource cleanup**: HTTP clients properly closed after use
- **Efficient formatting**: Streamlined HTML generation
- **Reduced duplication**: Consolidated conversion methods

## 🐛 Troubleshooting

### Common Issues

1. **"key file not found"**
   - Ensure the `key` file exists in the root directory
   - Check that it contains your valid OpenWeatherMap API key

2. **"Port already in use"**
   - Change the port number in `WebServer.groovy`
   - Or stop any other application using port 8080

3. **API errors**
   - Verify your API key is valid and active
   - Check your internet connection
   - Ensure you haven't exceeded API rate limits

4. **City not found**
   - Try different spellings or include country code (e.g., "London,UK")
   - Use major city names for better results

## 📝 API Rate Limits

Free OpenWeatherMap accounts have these limits:
- 1,000 API calls per day
- 60 API calls per minute

The application makes 2 API calls per weather request (current + forecast).

## 🚀 Future Enhancements

- Add weather data caching to reduce API calls
- Implement weather alerts and notifications
- Add more detailed weather information
- Include weather maps and radar
- Add user preferences persistence
- Implement weather history tracking

## 📄 License

This project is open source. Feel free to use and modify as needed.

---

**Note**: This is the refactored version with improved code organization. The original `SimpleWeb.groovy` file is maintained for compatibility but the new modular structure in the `src/` directory is recommended for development and maintenance.
