# GroovyWeatherApp

A modern weather application built with Groovy and Jetty, featuring a clean UI with animated backgrounds, error handling, and responsive design.

## 🚀 Features

- **Real-time Weather Data**: Current weather and 5-day forecast
- **Beautiful UI**: Animated gradient backgrounds and modern styling
- **Dual Timezone Support**: View weather in your timezone or local city time
- **Unit Conversion**: Toggle between Metric (°C) and Imperial (°F) units
- **Time Format Options**: 24-hour or 12-hour time display
- **Smart Error Handling**: Graceful error messages with categorized display
- **Input Enhancement**: Clear button for easy input management
- **Responsive Design**: Works on desktop and mobile devices

## 📁 Project Structure

```
GroovyWeatherApp/
├── src/
│   ├── WebServer.groovy          # Main server application
│   ├── WeatherApiClient.groovy   # API communication layer
│   ├── WeatherFormatter.groovy   # HTML formatting and utilities
│   ├── TemperatureConverter.groovy # Temperature conversion utilities
│   └── CountryCodeUtil.groovy    # Country code to name mapping
├── static/
│   ├── index.html               # Main HTML template
│   ├── styles.css              # All CSS styling
│   └── script.js               # Client-side JavaScript
├── config/                     # Configuration files (recommended)
├── IsoCountryCodes.csv         # Country code mappings
├── key                         # OpenWeatherMap API key
└── README.md                   # This file
```

## 🛠️ Setup Instructions

### Prerequisites
- Java 8 or higher
- Groovy 3.0 or higher
- OpenWeatherMap API key

### Installation

1. **Clone or download** the project to your local machine

2. **Get an API key** from [OpenWeatherMap](https://openweathermap.org/api)

3. **Create the API key file**:
   ```
   echo "your_api_key_here" > key
   ```

4. **Ensure the CSV file exists**:
   - Make sure `IsoCountryCodes.csv` is in the root directory
   - This file maps country codes to country names

### Running the Application

#### Option 1: Using the new modular structure
```bash
cd src
groovy WebServer.groovy
```

#### Option 2: Using the original file (deprecated)
```bash
groovy SimpleWeb.groovy
```

The server will start on `http://localhost:8080`

## 🎯 Usage

1. **Open your browser** to `http://localhost:8080`
2. **Enter a city name** in the search field
3. **Use the toggle buttons** at the top to:
   - Switch between your timezone and local city time
   - Toggle between Metric (°C) and Imperial (°F) units
   - Switch between 24-hour and 12-hour time format
4. **Click "Get Weather"** to fetch current weather and forecast

## 🔧 Configuration

### Port Configuration
To change the default port (8080), modify the `WebServer.groovy` file:
```groovy
def server = new WeatherWebServer(apiKey, 3000) // Change to desired port
```

### API Endpoints
The application uses OpenWeatherMap API endpoints:
- Current weather: `https://api.openweathermap.org/data/2.5/weather`
- 5-day forecast: `https://api.openweathermap.org/data/2.5/forecast`

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
