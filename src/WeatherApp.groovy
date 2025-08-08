@Grab('org.eclipse.jetty:jetty-server:9.4.48.v20220622')
@Grab('org.eclipse.jetty:jetty-servlet:9.4.48.v20220622')
@Grab('org.apache.httpcomponents:httpclient:4.5.14')
@Grab('org.apache.httpcomponents:httpcore:4.4.16')
@Grab('com.fasterxml.jackson.core:jackson-databind:2.15.2')

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import javax.servlet.http.*
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.TimeZone

/**
 * Country Code Utility for converting ISO country codes to names
 */
class CountryCodeUtil {
    private static Map<String, String> countryCodeMap = [:]
    
    static {
        loadCountryCodes()
    }
    
    private static void loadCountryCodes() {
        def csvFile = new File("../IsoCountryCodes.csv")
        if (!csvFile.exists()) {
            csvFile = new File("IsoCountryCodes.csv") // Try current directory
        }
        if (!csvFile.exists()) {
            println "Warning: 'IsoCountryCodes.csv' file not found. Using country codes instead of names."
            return
        }
        
        try {
            csvFile.eachLine { line, lineNumber ->
                if (lineNumber == 1) return // Skip header
                
                def parts = line.split(',')
                if (parts.length >= 2) {
                    def countryName = parts[0].trim()
                    def countryCode = parts[1].trim()
                    
                    // Remove quotes if present
                    if (countryName.startsWith('"') && countryName.endsWith('"')) {
                        countryName = countryName.substring(1, countryName.length() - 1)
                    }
                    if (countryCode.startsWith('"') && countryCode.endsWith('"')) {
                        countryCode = countryCode.substring(1, countryCode.length() - 1)
                    }
                    
                    countryCodeMap[countryCode] = countryName
                }
            }
            println "Loaded ${countryCodeMap.size()} country codes"
        } catch (Exception e) {
            println "Error loading country codes: ${e.message}"
        }
    }
    
    static String getCountryName(String countryCode) {
        if (!countryCode || countryCode.trim().isEmpty()) {
            return ""
        }
        
        def countryName = countryCodeMap[countryCode.toUpperCase()]
        return countryName ?: countryCode // Return original code if not found
    }
}

/**
 * US State Code Utility for converting state names/abbreviations to ISO 3166-2:US codes
 */
class USStateCodeUtil {
    private static Map<String, String> stateCodeMap = [:]
    private static Map<String, String> stateNameMap = [:]
    
    static {
        loadStateCodes()
    }
    
    private static void loadStateCodes() {
        def stateCodes = [
            'AL': 'Alabama', 'AK': 'Alaska', 'AZ': 'Arizona', 'AR': 'Arkansas',
            'CA': 'California', 'CO': 'Colorado', 'CT': 'Connecticut', 'DE': 'Delaware',
            'FL': 'Florida', 'GA': 'Georgia', 'HI': 'Hawaii', 'ID': 'Idaho',
            'IL': 'Illinois', 'IN': 'Indiana', 'IA': 'Iowa', 'KS': 'Kansas',
            'KY': 'Kentucky', 'LA': 'Louisiana', 'ME': 'Maine', 'MD': 'Maryland',
            'MA': 'Massachusetts', 'MI': 'Michigan', 'MN': 'Minnesota', 'MS': 'Mississippi',
            'MO': 'Missouri', 'MT': 'Montana', 'NE': 'Nebraska', 'NV': 'Nevada',
            'NH': 'New Hampshire', 'NJ': 'New Jersey', 'NM': 'New Mexico', 'NY': 'New York',
            'NC': 'North Carolina', 'ND': 'North Dakota', 'OH': 'Ohio', 'OK': 'Oklahoma',
            'OR': 'Oregon', 'PA': 'Pennsylvania', 'RI': 'Rhode Island', 'SC': 'South Carolina',
            'SD': 'South Dakota', 'TN': 'Tennessee', 'TX': 'Texas', 'UT': 'Utah',
            'VT': 'Vermont', 'VA': 'Virginia', 'WA': 'Washington', 'WV': 'West Virginia',
            'WI': 'Wisconsin', 'WY': 'Wyoming', 'DC': 'District of Columbia'
        ]
        
        // Create both code->name and name->code mappings
        stateCodes.each { code, name ->
            stateCodeMap[code] = name
            stateNameMap[name.toLowerCase()] = code
        }
        
        println "Loaded ${stateCodeMap.size()} US state codes"
    }
    
    static String getStateName(String stateCode) {
        if (!stateCode || stateCode.trim().isEmpty()) {
            return ""
        }
        return stateCodeMap[stateCode.toUpperCase()] ?: stateCode
    }
    
    static String getStateCode(String stateName) {
        if (!stateName || stateName.trim().isEmpty()) {
            return ""
        }
        return stateNameMap[stateName.toLowerCase().trim()] ?: ""
    }
    
    static boolean isValidStateCode(String stateCode) {
        return stateCode && stateCodeMap.containsKey(stateCode.toUpperCase())
    }
    
    static boolean isValidStateName(String stateName) {
        return stateName && stateNameMap.containsKey(stateName.toLowerCase().trim())
    }
    
    static String formatCityState(String city, String state) {
        if (!state || state.trim().isEmpty()) {
            return city
        }
        
        def stateCode = state.length() == 2 ? state.toUpperCase() : getStateCode(state)
        if (stateCode) {
            return "${city},${stateCode}"
        }
        return city
    }
    
    static Map<String, String> parseLocationQuery(String query) {
        def result = [city: query, state: "", country: ""]
        
        if (query.contains(',')) {
            def parts = query.split(',').collect { it.trim() }
            
            if (parts.size() >= 2) {
                result.city = parts[0]
                def secondPart = parts[1]
                
                // Check if second part is a US state code or name
                if (isValidStateCode(secondPart) || isValidStateName(secondPart)) {
                    result.state = secondPart
                    result.country = "US"
                    
                    // If there's a third part, it might be country
                    if (parts.size() >= 3) {
                        result.country = parts[2]
                    }
                } else {
                    // Assume it's a country code
                    result.country = secondPart
                    
                    // If there's a third part, it might be state (city,country,state format)
                    if (parts.size() >= 3 && result.country.toUpperCase() == "US") {
                        def thirdPart = parts[2]
                        if (isValidStateCode(thirdPart) || isValidStateName(thirdPart)) {
                            result.state = thirdPart
                        }
                    }
                }
            }
        }
        
        return result
    }
}

/**
 * Utility class for temperature conversion and formatting
 */
class TemperatureConverter {
    
    static String convertToFahrenheit(String tempStr) {
        def pattern = /(\d+\.?\d*)([°CF]+)/
        def matcher = tempStr =~ pattern
        
        if (matcher) {
            def temp = Double.parseDouble(matcher[0][1])
            def unit = matcher[0][2]
            
            if (unit.contains("C")) {
                def fahrenheit = (temp * 9/5) + 32
                return "${Math.round(fahrenheit)}°F"
            }
        }
        return tempStr
    }
    
    static String convertToCelsius(String tempStr) {
        def pattern = /(\d+\.?\d*)([°CF]+)/
        def matcher = tempStr =~ pattern
        
        if (matcher) {
            def temp = Double.parseDouble(matcher[0][1])
            def unit = matcher[0][2]
            
            if (unit.contains("F")) {
                def celsius = (temp - 32) * 5/9
                return "${Math.round(celsius)}°C"
            }
        }
        return tempStr
    }
    
    static String convertTemperatureWithFeelsLike(String value, boolean toFahrenheit) {
        def pattern = /(\d+\.?\d*)([°CF]+)(\s*\(feels like\s*)(\d+\.?\d*)([°CF]+\))/
        def matcher = value =~ pattern
        
        if (matcher) {
            def temp1 = Double.parseDouble(matcher[0][1])
            def unit1 = matcher[0][2]
            def middleText = matcher[0][3]
            def temp2 = Double.parseDouble(matcher[0][4])
            def unit2 = matcher[0][5]
            
            if (toFahrenheit && unit1.contains("C")) {
                def fahrenheit1 = Math.round((temp1 * 9/5) + 32)
                def fahrenheit2 = Math.round((temp2 * 9/5) + 32)
                return "${fahrenheit1}°F${middleText}${fahrenheit2}°F)"
            } else if (!toFahrenheit && unit1.contains("F")) {
                def celsius1 = Math.round((temp1 - 32) * 5/9)
                def celsius2 = Math.round((temp2 - 32) * 5/9)
                return "${celsius1}°C${middleText}${celsius2}°C)"
            }
        }
        return value
    }
    
    static String roundTemperature(String tempStr) {
        def pattern = /(\d+\.?\d*)([°CF]+)/
        def matcher = tempStr =~ pattern
        
        if (matcher) {
            def temp = Double.parseDouble(matcher[0][1])
            def unit = matcher[0][2]
            def rounded = Math.round(temp)
            return "${rounded}${unit}"
        }
        return tempStr
    }
    
    static String roundTemperatureWithFeelsLike(String value) {
        def pattern = /(\d+\.?\d*)([°CF]+)(\s*\(feels like\s*)(\d+\.?\d*)([°CF]+\))/
        def matcher = value =~ pattern
        
        if (matcher) {
            def temp1 = Math.round(Double.parseDouble(matcher[0][1]))
            def unit1 = matcher[0][2]
            def middleText = matcher[0][3]
            def temp2 = Math.round(Double.parseDouble(matcher[0][4]))
            def unit2 = matcher[0][5]
            return "${temp1}${unit1}${middleText}${temp2}${unit2}"
        }
        return value
    }
    
    static String extractMainTemperature(String value) {
        def pattern = /(\d+[°CF]+)/
        def matcher = value =~ pattern
        
        if (matcher) {
            return matcher[0][1]
        }
        return value
    }
    
    static String extractFeelsLikeTemperature(String value) {
        def pattern = /\(feels like\s*(\d+[°CF]+)\)/
        def matcher = value =~ pattern
        
        if (matcher) {
            return "feels like ${matcher[0][1]}"
        }
        return ""
    }
}

/**
 * Handles all weather data formatting and HTML generation
 */
class WeatherFormatter {
    
    static boolean isErrorResult(String result) {
        return result.startsWith("CITY_NOT_FOUND:") || result.startsWith("API_ERROR:") || 
               result.startsWith("RATE_LIMIT:") || result.startsWith("SERVICE_ERROR:") || 
               result.startsWith("CONNECTION_ERROR:") || result.startsWith("Error:")
    }
    
    static String extractCityName(String result) {
        if (isErrorResult(result)) {
            return ""
        }
        
        def lines = result.split('\n')
        for (line in lines) {
            if (line.startsWith("===") && line.contains("Current Weather for")) {
                def match = line =~ /=== Current Weather for (.+?) ===/
                if (match) {
                    return match[0][1]
                }
            }
        }
        return ""
    }
    
    static String convertTo12Hour(String time24) {
        try {
            def format24 = new SimpleDateFormat("HH:mm")
            def format12 = new SimpleDateFormat("hh:mm a")
            def date = format24.parse(time24)
            def result = format12.format(date)
            return result.toUpperCase()
        } catch (Exception e) {
            return time24
        }
    }
    
    static String getWeatherIcon(String description) {
        def desc = description.toLowerCase()
        
        if (desc.contains("rain") || desc.contains("drizzle")) {
            if (desc.contains("light")) return "🌦️"
            if (desc.contains("heavy")) return "🌧️"
            return "🌧️"
        }
        
        if (desc.contains("snow")) {
            return "❄️"
        }
        
        if (desc.contains("thunder") || desc.contains("storm")) {
            return "⛈️"
        }
        
        if (desc.contains("fog") || desc.contains("mist") || desc.contains("haze")) {
            return "🌫️"
        }
        
        if (desc.contains("clear") || desc.contains("sunny")) {
            return "☀️"
        }
        
        if (desc.contains("cloud")) {
            if (desc.contains("few") || desc.contains("scattered")) return "🌤️"
            if (desc.contains("broken") || desc.contains("overcast")) return "☁️"
            return "⛅"
        }
        
        if (desc.contains("wind")) {
            return "💨"
        }
        
        return "🌈"
    }
    
    static String convertPropertyToImperial(String property, String value) {
        switch (property) {
            case "Pressure":
                def pattern = /(\d+\.?\d*)\s*hPa/
                def matcher = value =~ pattern
                if (matcher) {
                    def pressure = Double.parseDouble(matcher[0][1])
                    def psi = Math.round(pressure * 0.0145038 * 100) / 100
                    return "${psi} psi"
                }
                break
                
            case "Wind Speed":
                def pattern = /(\d+\.?\d*)\s*m\/s/
                def matcher = value =~ pattern
                if (matcher) {
                    def speed = Double.parseDouble(matcher[0][1])
                    def mph = Math.round(speed * 2.237 * 10) / 10
                    return "${mph} mph"
                }
                break
                
            case "Visibility":
                def pattern = /(\d+\.?\d*)\s*km/
                def matcher = value =~ pattern
                if (matcher) {
                    def distance = Double.parseDouble(matcher[0][1])
                    def miles = Math.round(distance * 0.621371 * 100) / 100
                    return "${miles} miles"
                }
                break
        }
        return value
    }
    
    static String formatErrorAsHtml(String result) {
        if (result.startsWith("CITY_NOT_FOUND:")) {
            def message = result.substring(15).trim()
            return "<div class='error error-not-found'><div class='error-icon'>🏙️</div><div class='error-content'><h3>City Not Found</h3><p>${message}</p></div></div>"
        } else if (result.startsWith("API_ERROR:")) {
            def message = result.substring(10).trim()
            return "<div class='error error-api'><div class='error-icon'>🔑</div><div class='error-content'><h3>API Error</h3><p>${message}</p></div></div>"
        } else if (result.startsWith("RATE_LIMIT:")) {
            def message = result.substring(11).trim()
            return "<div class='error error-rate-limit'><div class='error-icon'>⏰</div><div class='error-content'><h3>Rate Limit</h3><p>${message}</p></div></div>"
        } else if (result.startsWith("SERVICE_ERROR:")) {
            def message = result.substring(14).trim()
            return "<div class='error error-service'><div class='error-icon'>🌐</div><div class='error-content'><h3>Service Error</h3><p>${message}</p></div></div>"
        } else if (result.startsWith("CONNECTION_ERROR:")) {
            def message = result.substring(17).trim()
            return "<div class='error error-connection'><div class='error-icon'>📡</div><div class='error-content'><h3>Connection Error</h3><p>${message}</p></div></div>"
        } else if (result.startsWith("Error:")) {
            def message = result.substring(6).trim()
            return "<div class='error error-general'><div class='error-icon'>⚠️</div><div class='error-content'><h3>Error</h3><p>${message}</p></div></div>"
        }
        return result
    }
}

/**
 * Weather API Client using OpenWeatherMap API
 */
class WeatherApiClient {
    
    private final String apiKey
    private final String baseUrl = "https://api.openweathermap.org/data/2.5"
    private final JsonSlurper jsonSlurper = new JsonSlurper()
    
    WeatherApiClient(String apiKey) {
        this.apiKey = apiKey
    }
    
    /**
     * Get coordinates for a city using OpenWeatherMap geocoding
     * This is used for weather lookups after city selection from Google Places
     */
    private def getCityCoordinates(String city) {
        def httpClient = HttpClients.createDefault()
        try {
            // Parse location query to handle state codes
            def locationInfo = USStateCodeUtil.parseLocationQuery(city)
            
            // Build the geocoding query with proper state code format
            def geocodingQuery = locationInfo.city
            if (locationInfo.state && locationInfo.country == "US") {
                def stateCode = locationInfo.state.length() == 2 ? 
                    locationInfo.state.toUpperCase() : 
                    USStateCodeUtil.getStateCode(locationInfo.state)
                if (stateCode) {
                    geocodingQuery = "${locationInfo.city},${stateCode},${locationInfo.country}"
                }
            } else if (locationInfo.country && locationInfo.country != "US") {
                geocodingQuery = "${locationInfo.city},${locationInfo.country}"
            }
            
            def httpGet = new HttpGet(url)
            def response = httpClient.execute(httpGet)
            def responseBody = EntityUtils.toString(response.entity)
            
            if (response.statusLine.statusCode == 200) {
                def geocodingData = jsonSlurper.parseText(responseBody)
                if (geocodingData && geocodingData.size() > 0) {
                    def location = geocodingData[0]
                    return [
                        lat: location.lat, 
                        lon: location.lon,
                        name: location.name,
                        country: location.country,
                        state: location.state ?: ""
                    ]
                }
            }
            
            return null
            
        } catch (Exception e) {
            println "Error getting coordinates for ${city}: ${e.message}"
            return null
        } finally {
            httpClient.close()
        }
    }
    
    def getCurrentWeather(String city) {
        // First, get coordinates for the city
        def coordinates = getCityCoordinates(city)
        if (!coordinates) {
            return "CITY_NOT_FOUND: We couldn't find coordinates for '${city}'. Please check the city name and try again."
        }
        
        def httpClient = HttpClients.createDefault()
        try {
            def url = "${baseUrl}/weather?lat=${coordinates.lat}&lon=${coordinates.lon}&appid=${apiKey}&units=metric"
            
            def httpGet = new HttpGet(url)
            def response = httpClient.execute(httpGet)
            def responseBody = EntityUtils.toString(response.entity)
            
            if (response.statusLine.statusCode == 200) {
                def weatherData = jsonSlurper.parseText(responseBody)
                return formatCurrentWeather(weatherData, coordinates)
            } else {
                return handleApiError(response.statusLine.statusCode, city)
            }
            
        } catch (Exception e) {
            return "CONNECTION_ERROR: Unable to connect to weather service. Please check your internet connection and try again."
        } finally {
            httpClient.close()
        }
    }
    
    def getForecast(String city) {
        // First, get coordinates for the city
        def coordinates = getCityCoordinates(city)
        if (!coordinates) {
            return "CITY_NOT_FOUND: We couldn't find coordinates for '${city}'. Please check the city name and try again."
        }
        
        def httpClient = HttpClients.createDefault()
        try {
            def url = "${baseUrl}/forecast?lat=${coordinates.lat}&lon=${coordinates.lon}&appid=${apiKey}&units=metric"
            
            def httpGet = new HttpGet(url)
            def response = httpClient.execute(httpGet)
            def responseBody = EntityUtils.toString(response.entity)
            
            if (response.statusLine.statusCode == 200) {
                def forecastData = jsonSlurper.parseText(responseBody)
                return formatForecastAsDaily(forecastData, coordinates)
            } else {
                return handleApiError(response.statusLine.statusCode, city)
            }
            
        } catch (Exception e) {
            return "CONNECTION_ERROR: Unable to connect to weather service. Please check your internet connection and try again."
        } finally {
            httpClient.close()
        }
    }
    
    def getCityTimezone(String city) {
        // First, get coordinates for the city
        def coordinates = getCityCoordinates(city)
        if (!coordinates) {
            return TimeZone.getDefault()
        }
        
        def httpClient = HttpClients.createDefault()
        try {
            def url = "${baseUrl}/weather?lat=${coordinates.lat}&lon=${coordinates.lon}&appid=${apiKey}"
            
            def httpGet = new HttpGet(url)
            def response = httpClient.execute(httpGet)
            def responseBody = EntityUtils.toString(response.entity)
            
            if (response.statusLine.statusCode == 200) {
                def weatherData = jsonSlurper.parseText(responseBody)
                def timezoneOffset = weatherData.timezone as Integer
                
                def offsetHours = Math.floor(timezoneOffset / 3600) as Integer
                def offsetMinutes = Math.floor(Math.abs((timezoneOffset % 3600) / 60)) as Integer
                def sign = timezoneOffset >= 0 ? "+" : "-"
                def timezoneId = "GMT${sign}${String.format('%02d', Math.abs(offsetHours))}:${String.format('%02d', offsetMinutes)}"
                
                return TimeZone.getTimeZone(timezoneId)
            }
        } catch (Exception e) {
            println "Error getting timezone for ${city}: ${e.message}"
        } finally {
            httpClient.close()
        }
        
        return TimeZone.getDefault()
    }
    
    private String handleApiError(int statusCode, String city) {
        switch (statusCode) {
            case 404:
                return "CITY_NOT_FOUND: We couldn't find weather data for '${city}'. Please check the city name and try again."
            case 401:
                return "API_ERROR: Authentication failed. Please check your API key."
            case 429:
                return "RATE_LIMIT: Too many requests. Please try again in a moment."
            default:
                return "SERVICE_ERROR: Weather service is temporarily unavailable. Please try again later."
        }
    }
    
    private def formatCurrentWeather(weatherData, coordinates = null) {
        def result = []
        
        // Use geocoding city name if available, otherwise fall back to weather API name
        def cityName = coordinates?.name ?: weatherData.name
        def countryCode = coordinates?.country ?: weatherData.sys.country
        def stateCode = coordinates?.state ?: ""
        
        // For the header, include state in city name for US cities (for backward compatibility)
        def headerCityName = cityName
        if (stateCode && countryCode == "US") {
            def stateName = USStateCodeUtil.getStateName(stateCode)
            if (stateName) {
                headerCityName = "${cityName}, ${stateName}"
            }
        }
        
        result << "=== Current Weather for ${headerCityName}, ${countryCode} ==="
        result << "Temperature: ${weatherData.main.temp}°C (feels like ${weatherData.main.feels_like}°C)"
        result << "Weather: ${weatherData.weather[0].main} - ${weatherData.weather[0].description}"
        result << "Humidity: ${weatherData.main.humidity}%"
        result << "Pressure: ${weatherData.main.pressure} hPa"
        result << "Wind Speed: ${weatherData.wind.speed} m/s"
        result << "Visibility: ${weatherData.visibility / 1000} km"
        result << ""
        return result.join("\n")
    }
    
    private def formatForecastAsDaily(forecastData, coordinates = null) {
        def dayGroups = [:]
        
        forecastData.list.each { forecast ->
            def date = new Date(forecast.dt * 1000L)
            def dateStr = date.format("yyyy-MM-dd")
            
            if (!dayGroups[dateStr]) {
                dayGroups[dateStr] = []
            }
            dayGroups[dateStr] << forecast
        }
        
        def sortedDays = dayGroups.keySet().sort().take(5)
        
        def result = []
        result << "DAILY_FORECAST_START"
        
        // Use geocoding city name if available, otherwise fall back to forecast API name
        def cityName = coordinates?.name ?: forecastData.city.name
        def countryCode = coordinates?.country ?: forecastData.city.country
        def stateName = ""
        
        // Include state information for US cities
        if (coordinates?.state && countryCode == "US") {
            stateName = USStateCodeUtil.getStateName(coordinates.state)
            if (stateName) {
                cityName = "${cityName}, ${stateName}"
            }
        }
        
        result << "City: ${cityName}, ${countryCode}"
        
        sortedDays.each { dateStr ->
            result << "DAY_START:${dateStr}"
            dayGroups[dateStr].each { forecast ->
                result << "${forecast.dt}: ${forecast.main.temp}°C, ${forecast.weather[0].description}"
            }
            result << "DAY_END"
        }
        
        result << "DAILY_FORECAST_END"
        return result.join("\n")
    }
}

/**
 * Main web server class - simplified and focused on server setup and request handling
 */
class WeatherWebServer {
    
    private final String openWeatherApiKey
    private final String googleApiKey
    private final int port
    private final WeatherApiClient weatherClient
    
    WeatherWebServer(String openWeatherApiKey, String googleApiKey, int port = 8080) {
        this.openWeatherApiKey = openWeatherApiKey
        this.googleApiKey = googleApiKey
        this.port = port
        this.weatherClient = new WeatherApiClient(openWeatherApiKey)
    }
    
    void start() {
        def server = new Server(port)
        def context = new ServletContextHandler()
        context.contextPath = "/"
        server.handler = context

        context.addServlet(new ServletHolder(createStaticFileServlet()), "/static/*")
        context.addServlet(new ServletHolder(createApiServlet()), "/api/*")
        context.addServlet(new ServletHolder(createMainServlet()), "/*")

        server.start()
        println "Server started at http://localhost:${port}"
        server.join()
    }
    
    private HttpServlet createStaticFileServlet() {
        return new HttpServlet() {
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
                def path = req.requestURI
                if (path.startsWith("/static/")) {
                    def fileName = path.substring(8)
                    def file = new File("../static/${fileName}")
                    if (!file.exists()) {
                        file = new File("static/${fileName}") // Try current directory
                    }
                    
                    if (file.exists() && file.isFile()) {
                        if (fileName.endsWith(".css")) {
                            resp.contentType = "text/css"
                        } else if (fileName.endsWith(".js")) {
                            resp.contentType = "application/javascript"
                        } else if (fileName.endsWith(".html")) {
                            resp.contentType = "text/html"
                        }
                        resp.writer.write(file.text)
                    } else {
                        resp.status = 404
                        resp.writer.write("File not found: ${fileName}")
                    }
                } else {
                    resp.status = 404
                    resp.writer.write("Not found")
                }
            }
        }
    }
    
    private HttpServlet createApiServlet() {
        return new HttpServlet() {
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
                def path = req.requestURI
                
                if (path.startsWith("/api/cities")) {
                    handleCitySuggestions(req, resp)
                } else if (path.startsWith("/api/place-details")) {
                    handlePlaceDetails(req, resp)
                } else {
                    resp.status = 404
                    resp.writer.write("API endpoint not found")
                }
            }
            
            private void handleCitySuggestions(HttpServletRequest req, HttpServletResponse resp) {
                def query = req.getParameter("q")
                
                if (!query || query.trim().length() < 2) {
                    resp.contentType = "application/json"
                    resp.writer.write("[]")
                    return
                }
                
                def httpClient = HttpClients.createDefault()
                try {
                    // Use Google Places Autocomplete API with restriction to cities only
                    def url = "https://places.googleapis.com/v1/places:autocomplete"
                    
                    def httpPost = new HttpPost(url)
                    httpPost.setHeader("Content-Type", "application/json")
                    httpPost.setHeader("X-Goog-Api-Key", googleApiKey)
                    
                    // Create request body for Google Places API
                    def requestBody = [
                        input: query.trim(),
                        includedPrimaryTypes: ["(cities)"],
                        languageCode: "en"
                    ]
                    
                    def requestJson = new JsonBuilder(requestBody).toString()
                    httpPost.setEntity(new StringEntity(requestJson, "UTF-8"))
                    
                    def response = httpClient.execute(httpPost)
                    def responseBody = EntityUtils.toString(response.entity)
                    
                    if (response.statusLine.statusCode == 200) {
                        def jsonSlurper = new JsonSlurper()
                        def placesResponse = jsonSlurper.parseText(responseBody)
                        
                        // Transform Google Places response to our format
                        def suggestions = []
                        if (placesResponse.suggestions) {
                            suggestions = placesResponse.suggestions.collect { suggestion ->
                                def placePrediction = suggestion.placePrediction
                                if (placePrediction) {
                                    // Extract city name and location details
                                    def mainText = placePrediction.text?.text ?: placePrediction.description ?: ""
                                    def secondaryText = placePrediction.structuredFormat?.secondaryText?.text ?: ""
                                    
                                    // For display, use the full description
                                    def displayName = mainText
                                    if (secondaryText && !mainText.contains(secondaryText)) {
                                        displayName = "${mainText}, ${secondaryText}"
                                    }
                                    
                                    // Extract basic location info from the description
                                    def parts = displayName.split(",").collect { it.trim() }
                                    def cityName = parts[0]
                                    def country = parts.size() > 1 ? parts[-1] : ""
                                    def state = parts.size() > 2 ? parts[-2] : ""
                                    
                                    [
                                        name: cityName,
                                        country: country,
                                        state: state,
                                        display: displayName,
                                        searchValue: displayName,
                                        placeId: placePrediction.placeId ?: "",
                                        // Note: Google Places Autocomplete doesn't provide lat/lon directly
                                        lat: null,
                                        lon: null
                                    ]
                                }
                            }.findAll { it != null }
                        }
                        
                        resp.contentType = "application/json"
                        resp.writer.write(new JsonBuilder(suggestions).toString())
                    } else {
                        println "Google Places API error: ${response.statusLine.statusCode} - ${responseBody}"
                        resp.status = 500
                        resp.writer.write("[]")
                    }
                    
                } catch (Exception e) {
                    println "Error fetching city suggestions from Google Places: ${e.message}"
                    e.printStackTrace()
                    resp.status = 500
                    resp.writer.write("[]")
                } finally {
                    httpClient.close()
                }
            }
            
            private void handlePlaceDetails(HttpServletRequest req, HttpServletResponse resp) {
                def placeId = req.getParameter("place_id")
                
                if (!placeId || placeId.trim().isEmpty()) {
                    resp.status = 400
                    resp.writer.write('{"error": "place_id parameter required"}')
                    return
                }
                
                def httpClient = HttpClients.createDefault()
                try {
                    // Use Google Places Details API to get coordinates
                    def url = "https://places.googleapis.com/v1/places/${placeId}"
                    
                    def httpGet = new HttpGet(url)
                    httpGet.setHeader("X-Goog-Api-Key", googleApiKey)
                    httpGet.setHeader("X-Goog-FieldMask", "location")
                    
                    def response = httpClient.execute(httpGet)
                    def responseBody = EntityUtils.toString(response.entity)
                    
                    if (response.statusLine.statusCode == 200) {
                        def jsonSlurper = new JsonSlurper()
                        def placeDetails = jsonSlurper.parseText(responseBody)
                        
                        if (placeDetails.location) {
                            def coordinates = [
                                lat: placeDetails.location.latitude,
                                lon: placeDetails.location.longitude
                            ]
                            resp.contentType = "application/json"
                            resp.writer.write(new JsonBuilder(coordinates).toString())
                        } else {
                            resp.status = 404
                            resp.writer.write('{"error": "Location not found"}')
                        }
                    } else {
                        println "Google Places Details API error: ${response.statusLine.statusCode} - ${responseBody}"
                        resp.status = 500
                        resp.writer.write('{"error": "Failed to fetch place details"}')
                    }
                    
                } catch (Exception e) {
                    println "Error fetching place details: ${e.message}"
                    e.printStackTrace()
                    resp.status = 500
                    resp.writer.write('{"error": "Internal server error"}')
                } finally {
                    httpClient.close()
                }
            }
        }
    }
    
    private HttpServlet createMainServlet() {
        return new HttpServlet() {
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
                handleRequest(req, resp)
            }
            
            protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
                handleRequest(req, resp)
            }
            
            private void handleRequest(HttpServletRequest req, HttpServletResponse resp) {
                def city = req.getParameter("city")
                def timezone = req.getParameter("timezone") ?: "your"
                def units = req.getParameter("units") ?: "metric"
                def timeformat = req.getParameter("timeformat") ?: "24hour"
                
                def currentTableHtml = ""
                def forecastTableHtml = ""
                def cityName = ""
                def cityTimezone = TimeZone.getDefault()
                
                if (city && !city.trim().isEmpty()) {
                    def currentResult = weatherClient.getCurrentWeather(city)
                    
                    if (WeatherFormatter.isErrorResult(currentResult)) {
                        currentTableHtml = WeatherFormatter.formatErrorAsHtml(currentResult)
                        forecastTableHtml = ""
                    } else {
                        def forecastResult = weatherClient.getForecast(city)
                        cityTimezone = weatherClient.getCityTimezone(city)
                        
                        cityName = WeatherFormatter.extractCityName(currentResult)
                        
                        currentTableHtml = formatResultAsTable(currentResult, "current", cityTimezone)
                        
                        if (WeatherFormatter.isErrorResult(forecastResult)) {
                            forecastTableHtml = "<div class='forecast-error-notice'>Forecast data is temporarily unavailable for this location.</div>"
                        } else {
                            forecastTableHtml = formatResultAsTable(forecastResult, "forecast", cityTimezone)
                        }
                    }
                } else {
                    currentTableHtml = ""
                    forecastTableHtml = ""
                }
                
                resp.contentType = "text/html"
                def htmlContent = loadHtmlTemplate(city, timezone, units, timeformat, currentTableHtml, forecastTableHtml, cityName, cityTimezone)
                resp.writer.println(htmlContent)
            }
        }
    }
    
    private String loadHtmlTemplate(String city, String timezone, String units, String timeformat, 
                                  String currentTableHtml, String forecastTableHtml, String cityName, TimeZone cityTimezone) {
        def templateFile = new File("../static/index.html")
        if (!templateFile.exists()) {
            templateFile = new File("static/index.html") // Try current directory
        }
        if (!templateFile.exists()) {
            return "<html><body><h1>Template file not found</h1></body></html>"
        }
        
        def template = templateFile.text
        
        template = template.replace("{{CITY_VALUE}}", city ?: '')
        template = template.replace("{{TIMEZONE_VALUE}}", timezone)
        template = template.replace("{{UNITS_VALUE}}", units)
        template = template.replace("{{TIMEFORMAT_VALUE}}", timeformat)
        template = template.replace("{{CITY_HEADER}}", '')
        template = template.replace("{{CURRENT_WEATHER_CONTENT}}", currentTableHtml)
        template = template.replace("{{FORECAST_CONTENT}}", forecastTableHtml)
        
        return template
    }
    
    private String formatResultAsTable(String result, String type, TimeZone cityTimezone) {
        if (WeatherFormatter.isErrorResult(result)) {
            return WeatherFormatter.formatErrorAsHtml(result)
        }
        
        def lines = result.split('\n')
        def yourTimezoneName = TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT)
        def cityTimezoneName = cityTimezone.getDisplayName(false, TimeZone.SHORT)
        
        if (type == "forecast") {
            if (result.contains("DAILY_FORECAST_START")) {
                return formatDailyForecastTables(result, cityTimezone, yourTimezoneName, cityTimezoneName)
            }
        } else {
            return formatCurrentWeatherHtml(lines, cityTimezone, yourTimezoneName, cityTimezoneName)
        }
        
        return ""
    }
    
    private String formatCurrentWeatherHtml(def lines, TimeZone cityTimezone, String yourTimezoneName, String cityTimezoneName) {
        def weatherData = [:]
        def cityName = ""
        def stateName = ""
        def countryCode = ""
        
        lines.each { line ->
            line = line.trim()
            if (line.startsWith("===")) {
                def match = line =~ /=== Current Weather for (.+?) ===/
                if (match) {
                    def fullLocation = match[0][1]
                    def locationParts = fullLocation.split(",")
                    
                    if (locationParts.length >= 3) {
                        // Format: "City, State, Country" (e.g., "Austin, Texas, US")
                        cityName = locationParts[0].trim()
                        stateName = locationParts[1].trim()
                        countryCode = locationParts[2].trim()
                    } else if (locationParts.length == 2) {
                        // Format: "City, Country" (e.g., "London, GB")
                        cityName = locationParts[0].trim()
                        stateName = ""
                        countryCode = locationParts[1].trim()
                    } else {
                        // Single location name
                        cityName = fullLocation
                        stateName = ""
                        countryCode = ""
                    }
                }
            } else if (line.contains(":") && !line.isEmpty()) {
                def parts = line.split(":", 2)
                if (parts.length == 2) {
                    def property = parts[0].trim()
                    def value = parts[1].trim()
                    weatherData[property] = value
                }
            }
        }
        
        def now = new Date()
        def yourFormat = new SimpleDateFormat("HH:mm")
        def cityFormat = new SimpleDateFormat("HH:mm")
        
        yourFormat.timeZone = TimeZone.getDefault()
        cityFormat.timeZone = cityTimezone
        
        def yourTime = yourFormat.format(now)
        def cityTime = cityFormat.format(now)
        def yourTime12 = WeatherFormatter.convertTo12Hour(yourTime)
        def cityTime12 = WeatherFormatter.convertTo12Hour(cityTime)
        
        def html = new StringBuilder()
        
        html.append("<div class='current-weather-container'>")
        html.append("<div class='current-weather-top'>")
        
        // Column 1: City, State (if present), Country, Time
        html.append("<div class='location-time-column'>")
        html.append("<div class='location-info'>")
        html.append("<div class='city-name-display'>${cityName}</div>")
        if (stateName && !stateName.isEmpty()) {
            html.append("<div class='state-name-display'>${stateName}</div>")
        }
        if (countryCode) {
            def countryName = CountryCodeUtil.getCountryName(countryCode)
            // Special case for UK to show shorter name
            if (countryCode == "GB" && countryName.startsWith("United Kingdom")) {
                countryName = "United Kingdom"
            }
            html.append("<div class='country-code-display'>${countryName}</div>")        
        }
        html.append("</div>")
        html.append("<div class='current-time-display'>")
        html.append("<span class='your-time'>")
        html.append("<span class='time-24h'>${yourTime} (${yourTimezoneName})</span>")
        html.append("<span class='time-12h' style='display: none;'>${yourTime12} (${yourTimezoneName})</span>")
        html.append("</span>")
        html.append("<span class='local-time' style='display: none;'>")
        html.append("<span class='time-24h'>${cityTime} (${cityTimezoneName})</span>")
        html.append("<span class='time-12h' style='display: none;'>${cityTime12} (${cityTimezoneName})</span>")
        html.append("</span>")
        html.append("</div>")
        html.append("</div>")
        
        // Column 2: Temperature
        html.append("<div class='temperature-column'>")
        if (weatherData["Temperature"]) {
            def tempValue = weatherData["Temperature"]
            def metricValue = TemperatureConverter.roundTemperatureWithFeelsLike(tempValue)
            def imperialValue = TemperatureConverter.convertTemperatureWithFeelsLike(tempValue, true)
            
            def metricMainTemp = TemperatureConverter.extractMainTemperature(metricValue)
            def metricFeelsLike = TemperatureConverter.extractFeelsLikeTemperature(metricValue)
            def imperialMainTemp = TemperatureConverter.extractMainTemperature(imperialValue)
            def imperialFeelsLike = TemperatureConverter.extractFeelsLikeTemperature(imperialValue)
            
            html.append("<span class='metric-units'>")
            html.append("<div class='temperature-display'>")
            html.append("<div class='main-temperature'>${metricMainTemp}</div>")
            html.append("<div class='feels-like-temperature'>${metricFeelsLike}</div>")
            html.append("</div>")
            html.append("</span>")
            html.append("<span class='imperial-units' style='display: none;'>")
            html.append("<div class='temperature-display'>")
            html.append("<div class='main-temperature'>${imperialMainTemp}</div>")
            html.append("<div class='feels-like-temperature'>${imperialFeelsLike}</div>")
            html.append("</div>")
            html.append("</span>")
        }
        html.append("</div>")
        
        // Column 3: Weather
        html.append("<div class='weather-column'>")
        if (weatherData["Weather"]) {
            def weatherValue = weatherData["Weather"]
            def descParts = weatherValue.split(" - ", 2)
            def desc = descParts.length > 1 ? descParts[1] : weatherValue
            def icon = WeatherFormatter.getWeatherIcon(desc)
            
            html.append("<div class='weather-display'>")
            html.append("<div class='weather-icon-large'>${icon}</div>")
            html.append("<div class='weather-description'>${desc}</div>")
            html.append("</div>")
        }
        html.append("</div>")
        
        html.append("</div>") // End top row
        
        // Bottom section
        html.append("<div class='current-weather-bottom'>")
        
        // Left column
        html.append("<div class='weather-details-left'>")
        if (weatherData["Humidity"]) {
            html.append("<div class='weather-detail'>")
            html.append("<span class='detail-label'>Humidity:</span>")
            html.append("<span class='detail-value'>${weatherData['Humidity']}</span>")
            html.append("</div>")
        }
        if (weatherData["Pressure"]) {
            html.append("<div class='weather-detail'>")
            html.append("<span class='detail-label'>Pressure:</span>")
            html.append("<span class='detail-value'>")
            html.append("<span class='metric-units'>${weatherData['Pressure']}</span>")
            html.append("<span class='imperial-units' style='display: none;'>${WeatherFormatter.convertPropertyToImperial('Pressure', weatherData['Pressure'])}</span>")
            html.append("</span>")
            html.append("</div>")
        }
        html.append("</div>")
        
        // Right column
        html.append("<div class='weather-details-right'>")
        if (weatherData["Wind Speed"]) {
            html.append("<div class='weather-detail'>")
            html.append("<span class='detail-label'>Wind Speed:</span>")
            html.append("<span class='detail-value'>")
            html.append("<span class='metric-units'>${weatherData['Wind Speed']}</span>")
            html.append("<span class='imperial-units' style='display: none;'>${WeatherFormatter.convertPropertyToImperial('Wind Speed', weatherData['Wind Speed'])}</span>")
            html.append("</span>")
            html.append("</div>")
        }
        if (weatherData["Visibility"]) {
            html.append("<div class='weather-detail'>")
            html.append("<span class='detail-label'>Visibility:</span>")
            html.append("<span class='detail-value'>")
            html.append("<span class='metric-units'>${weatherData['Visibility']}</span>")
            html.append("<span class='imperial-units' style='display: none;'>${WeatherFormatter.convertPropertyToImperial('Visibility', weatherData['Visibility'])}</span>")
            html.append("</span>")
            html.append("</div>")
        }
        html.append("</div>")
        
        html.append("</div>") // End bottom row
        html.append("</div>") // End container
        
        return html.toString()
    }
    
    private String formatDailyForecastTables(String result, TimeZone cityTimezone, String yourTimezoneName, String cityTimezoneName) {
        def lines = result.split('\n')
        def html = new StringBuilder()
        
        html.append("<div class='daily-forecast-container'>")
        
        def cityName = ""
        def currentDay = null
        def dayForecasts = []
        
        lines.each { line ->
            line = line.trim()
            
            if (line.startsWith("City:")) {
                cityName = line.substring(5).trim()
            } else if (line.startsWith("DAY_START:")) {
                if (currentDay != null) {
                    dayForecasts << currentDay
                }
                def dateStr = line.substring(10).trim()
                currentDay = [date: dateStr, forecasts: []]
            } else if (line == "DAY_END") {
                // Day ended
            } else if (line.contains(":") && currentDay != null) {
                def timePattern = /^(\d+):\s*(.+)$/
                def matcher = line =~ timePattern
                
                if (matcher) {
                    def timestampStr = matcher[0][1]
                    def weatherInfo = matcher[0][2]
                    def tempAndDesc = weatherInfo.split(",", 2)
                    def tempRaw = tempAndDesc.length > 0 ? tempAndDesc[0].trim() : ""
                    def desc = tempAndDesc.length > 1 ? tempAndDesc[1].trim() : ""
                    
                    currentDay.forecasts << [timestamp: Long.parseLong(timestampStr), temp: tempRaw, description: desc]
                }
            }
        }
        
        if (currentDay != null) {
            dayForecasts << currentDay
        }
        
        dayForecasts.each { day ->
            html.append("<div class='daily-table'>")
            
            def date = Date.parse("yyyy-MM-dd", day.date)
            def dayName = date.format("EEEE")
            def displayDate = date.format("MMM dd")
            
            html.append("<h3 class='day-header'>${dayName}<br><span class='date-small'>${displayDate}</span></h3>")
            html.append("<table class='compact-forecast'>")
            
            day.forecasts.each { forecast ->
                def forecastDate = new Date(forecast.timestamp * 1000L)
                
                def userFormat = new SimpleDateFormat("HH:mm")
                userFormat.timeZone = TimeZone.getDefault()
                def userTimeStr = userFormat.format(forecastDate)
                def user12HourStr = WeatherFormatter.convertTo12Hour(userTimeStr)
                
                def cityFormat = new SimpleDateFormat("HH:mm")
                cityFormat.timeZone = cityTimezone
                def cityTimeStr = cityFormat.format(forecastDate)
                def city12HourStr = WeatherFormatter.convertTo12Hour(cityTimeStr)
                
                def tempMetric = TemperatureConverter.roundTemperature(forecast.temp)
                def tempImperial = TemperatureConverter.convertToFahrenheit(forecast.temp)
                def weatherIcon = WeatherFormatter.getWeatherIcon(forecast.description)
                
                html.append("<tr>")
                html.append("<td class='time-cell'>")
                html.append("<span class='your-time'>")
                html.append("<span class='time-24h'>${userTimeStr}</span>")
                html.append("<span class='time-12h' style='display: none;'>${user12HourStr}</span>")
                html.append("</span>")
                html.append("<span class='local-time' style='display: none;'>")
                html.append("<span class='time-24h'>${cityTimeStr}</span>")
                html.append("<span class='time-12h' style='display: none;'>${city12HourStr}</span>")
                html.append("</span>")
                html.append("</td>")
                html.append("<td class='temp-cell'>")
                html.append("<span class='metric-units'>${tempMetric}</span>")
                html.append("<span class='imperial-units' style='display: none;'>${tempImperial}</span>")
                html.append("</td>")
                html.append("<td class='weather-cell'>${weatherIcon}</td>")
                html.append("</tr>")
            }
            
            html.append("</table>")
            html.append("</div>")
        }
        
        html.append("</div>")
        return html.toString()
    }
}

// Read API keys from files
def openWeatherApiKey = ""
def googleApiKey = ""

// Try to read OpenWeatherMap API key
def openWeatherKeyFile = new File("../key")
if (!openWeatherKeyFile.exists()) {
    openWeatherKeyFile = new File("key") // Try current directory
}
if (openWeatherKeyFile.exists()) {
    openWeatherApiKey = openWeatherKeyFile.text.trim()
}

// Try to read Google API key from separate file
def googleKeyFile = new File("../google-key")
if (!googleKeyFile.exists()) {
    googleKeyFile = new File("google-key") // Try current directory
}
if (googleKeyFile.exists()) {
    googleApiKey = googleKeyFile.text.trim()
}

// Validate API keys
if (openWeatherApiKey.isEmpty()) {
    println "Error: OpenWeatherMap API key not found"
    println "Please create a file named 'key' containing your OpenWeatherMap API key"
    println "Or create separate 'key' and 'google-key' files"
    System.exit(1)
}

if (googleApiKey.isEmpty()) {
    println "Error: Google Places API key not found"
    println "Please create a file named 'google-key' containing your Google Places API key"
    println "Or add the Google API key as the second line in the 'key' file"
    System.exit(1)
}

// Start the server
def server = new WeatherWebServer(openWeatherApiKey, googleApiKey)
server.start()
