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
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.net.URLEncoder
import java.util.TimeZone
import java.text.SimpleDateFormat

/**
 * Country Code Utility for converting ISO country codes to names
 */
class CountryCodeUtil {
    private static Map<String, String> countryCodeMap = [:]
    
    static {
        loadCountryCodes()
    }
    
    private static void loadCountryCodes() {
        def csvFile = new File("IsoCountryCodes.csv")
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
     * Get current weather for a city
     */
    def getCurrentWeather(String city) {
        try {
            def encodedCity = URLEncoder.encode(city, "UTF-8")
            def url = "${baseUrl}/weather?q=${encodedCity}&appid=${apiKey}&units=metric"
            
            def httpClient = HttpClients.createDefault()
            def httpGet = new HttpGet(url)
            
            def response = httpClient.execute(httpGet)
            def responseBody = EntityUtils.toString(response.entity)
            
            if (response.statusLine.statusCode == 200) {
                def weatherData = jsonSlurper.parseText(responseBody)
                return formatCurrentWeather(weatherData)
            } else {
                return "Error: ${response.statusLine.statusCode} - ${responseBody}"
            }
            
        } catch (Exception e) {
            return "Error fetching weather data: ${e.message}"
        }
    }
    
    /**
     * Get 5-day weather forecast for a city
     */
    def getForecast(String city) {
        try {
            def encodedCity = URLEncoder.encode(city, "UTF-8")
            def url = "${baseUrl}/forecast?q=${encodedCity}&appid=${apiKey}&units=metric"
            
            def httpClient = HttpClients.createDefault()
            def httpGet = new HttpGet(url)
            
            def response = httpClient.execute(httpGet)
            def responseBody = EntityUtils.toString(response.entity)
            
            if (response.statusLine.statusCode == 200) {
                def forecastData = jsonSlurper.parseText(responseBody)
                return formatForecastAsDaily(forecastData)
            } else {
                return "Error: ${response.statusLine.statusCode} - ${responseBody}"
            }
            
        } catch (Exception e) {
            return "Error fetching forecast data: ${e.message}"
        }
    }
    
    /**
     * Format current weather data for display
     */
    private def formatCurrentWeather(weatherData) {
        def result = []
        result << "=== Current Weather for ${weatherData.name}, ${weatherData.sys.country} ==="
        result << "Temperature: ${weatherData.main.temp}°C (feels like ${weatherData.main.feels_like}°C)"
        result << "Weather: ${weatherData.weather[0].main} - ${weatherData.weather[0].description}"
        result << "Humidity: ${weatherData.main.humidity}%"
        result << "Pressure: ${weatherData.main.pressure} hPa"
        result << "Wind Speed: ${weatherData.wind.speed} m/s"
        result << "Visibility: ${weatherData.visibility / 1000} km"
        result << ""
        return result.join("\n")
    }
    
    /**
     * Format forecast data for display as 5 side-by-side daily tables
     */
    private def formatForecastAsDaily(forecastData) {
        def dayGroups = [:]
        
        // Group forecast data by day
        forecastData.list.each { forecast ->
            def date = new Date(forecast.dt * 1000L)
            def dateStr = date.format("yyyy-MM-dd")
            
            if (!dayGroups[dateStr]) {
                dayGroups[dateStr] = []
            }
            dayGroups[dateStr] << forecast
        }
        
        // Take only the first 5 days
        def sortedDays = dayGroups.keySet().sort().take(5)
        
        def result = []
        result << "DAILY_FORECAST_START"
        result << "City: ${forecastData.city.name}, ${forecastData.city.country}"
        
        sortedDays.each { dateStr ->
            result << "DAY_START:${dateStr}"
            dayGroups[dateStr].each { forecast ->
                def date = new Date(forecast.dt * 1000L)
                // Store the Unix timestamp for proper timezone conversion later
                result << "${forecast.dt}: ${forecast.main.temp}°C, ${forecast.weather[0].description}"
            }
            result << "DAY_END"
        }
        
        result << "DAILY_FORECAST_END"
        return result.join("\n")
    }

    /**
     * Format forecast data for display
     */
    private def formatForecast(forecastData) {
        def result = []
        result << "=== 5-Day Forecast for ${forecastData.city.name}, ${forecastData.city.country} ==="
        result << ""
        
        def currentDate = ""
        forecastData.list.each { forecast ->
            def date = new Date(forecast.dt * 1000L)
            def dateStr = date.format("yyyy-MM-dd")
            def timeStr = date.format("HH:mm")
            
            if (dateStr != currentDate) {
                if (currentDate != "") result << ""
                result << "--- ${dateStr} ---"
                currentDate = dateStr
            }
            
            result << "${timeStr}: ${forecast.main.temp}°C, ${forecast.weather[0].description}"
        }
        
        return result.join("\n")
    }
}

// Read API key from file
def apiKeyFile = new File("key")
if (!apiKeyFile.exists()) {
    println "Error: 'key' file not found in current directory"
    println "Please create a file named 'key' containing your OpenWeatherMap API key"
    System.exit(1)
}

def apiKey = apiKeyFile.text.trim()
if (apiKey.isEmpty()) {
    println "Error: 'key' file is empty"
    System.exit(1)
}

def server = new Server(8080)
def context = new ServletContextHandler()
context.contextPath = "/"
server.handler = context

// Add static file serving
context.addServlet(new ServletHolder(new HttpServlet() {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        def path = req.requestURI
        if (path.startsWith("/static/")) {
            def fileName = path.substring(8) // Remove "/static/" prefix
            def file = new File("static/${fileName}")
            
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
}), "/static/*")

// Function to read and process HTML template
def loadHtmlTemplate(String city, String timezone, String units, String timeformat, String currentTableHtml, String forecastTableHtml, String cityName, TimeZone cityTimezone) {
    def templateFile = new File("static/index.html")
    if (!templateFile.exists()) {
        return "<html><body><h1>Template file not found</h1></body></html>"
    }
    
    def template = templateFile.text
    
    // Replace placeholders
    template = template.replace("{{CITY_VALUE}}", city ?: '')
    template = template.replace("{{TIMEZONE_VALUE}}", timezone)
    template = template.replace("{{UNITS_VALUE}}", units)
    template = template.replace("{{TIMEFORMAT_VALUE}}", timeformat)
    
    // Add conditional headers and content
    if (city && !city.trim().isEmpty()) {
        // City header is now integrated into the current weather display
        template = template.replace("{{CITY_HEADER}}", '')
    } else {
        template = template.replace("{{CITY_HEADER}}", '')
    }
    
    template = template.replace("{{CURRENT_WEATHER_CONTENT}}", currentTableHtml)
    template = template.replace("{{FORECAST_CONTENT}}", forecastTableHtml)
    
    return template
}

context.addServlet(new ServletHolder(new HttpServlet() {
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
            // Always fetch weather data in metric units - conversion handled client-side
            def weatherClient = new WeatherApiClient(apiKey) // Simplified constructor
            def currentResult = weatherClient.getCurrentWeather(city)
            def forecastResult = weatherClient.getForecast(city)
            cityTimezone = getCityTimezone(city, apiKey)
            
            // Extract city name from current weather result
            cityName = extractCityName(currentResult)
            
            // Parse both results to create table HTML - always pass false for useImperial
            currentTableHtml = formatResultAsTable(currentResult, "current", cityTimezone, false)
            forecastTableHtml = formatResultAsTable(forecastResult, "forecast", cityTimezone, false)
        } else {
            // Hide content when no city is provided
            currentTableHtml = ""
            forecastTableHtml = ""
        }
        
        resp.contentType = "text/html"
        def htmlContent = loadHtmlTemplate(city, timezone, units, timeformat, currentTableHtml, forecastTableHtml, cityName, cityTimezone)
        resp.writer.println(htmlContent)
    }
}), "/*")

def extractCityName(String result) {
    if (result.startsWith("Error:")) {
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

def getCityTimezone(String city, String apiKey) {  // Add apiKey parameter
    try {
        // Get timezone from OpenWeatherMap API
        def encodedCity = URLEncoder.encode(city, "UTF-8")
        def url = "https://api.openweathermap.org/data/2.5/weather?q=${encodedCity}&appid=${apiKey}"
        
        def httpClient = HttpClients.createDefault()
        def httpGet = new HttpGet(url)
        def response = httpClient.execute(httpGet)
        def responseBody = EntityUtils.toString(response.entity)
        
        if (response.statusLine.statusCode == 200) {
            def jsonSlurper = new JsonSlurper()
            def weatherData = jsonSlurper.parseText(responseBody)
            def timezoneOffset = weatherData.timezone as Integer // Ensure it's an integer
            
            // Create a custom timezone based on the offset
            def offsetHours = Math.floor(timezoneOffset / 3600) as Integer
            def offsetMinutes = Math.floor(Math.abs((timezoneOffset % 3600) / 60)) as Integer
            def sign = timezoneOffset >= 0 ? "+" : "-"
            def timezoneId = "GMT${sign}${String.format('%02d', Math.abs(offsetHours))}:${String.format('%02d', offsetMinutes)}"
            
            return TimeZone.getTimeZone(timezoneId)
        }
    } catch (Exception e) {
        println "Error getting timezone for ${city}: ${e.message}"
    }
    
    return TimeZone.getDefault() // Fallback to system timezone
}

def formatResultAsTable(String result, String type, TimeZone cityTimezone, boolean useImperial) {
    if (result.startsWith("Error:")) {
        return "<div class='error'>${result}</div>"
    }
    
    def lines = result.split('\n')
    def html = new StringBuilder()
    
    // Get timezone display names
    def yourTimezoneName = TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT)
    def cityTimezoneName = cityTimezone.getDisplayName(false, TimeZone.SHORT)
    
    if (type == "forecast") {
        // Check if this is the new daily format
        if (result.contains("DAILY_FORECAST_START")) {
            return formatDailyForecastTables(result, cityTimezone, yourTimezoneName, cityTimezoneName)
        } else {
            // Legacy format - keep existing code for compatibility
            return formatLegacyForecastTable(result, cityTimezone, yourTimezoneName, cityTimezoneName)
        }
        
    } else {
        // Extract weather data from lines
        def weatherData = [:]
        def cityName = ""
        def countryCode = ""
        
        lines.each { line ->
            line = line.trim()
            if (line.startsWith("===")) {
                // Extract city and country from header
                def match = line =~ /=== Current Weather for (.+?) ===/
                if (match) {
                    def fullLocation = match[0][1]
                    def locationParts = fullLocation.split(",", 2)
                    cityName = locationParts.length > 0 ? locationParts[0].trim() : fullLocation
                    countryCode = locationParts.length > 1 ? locationParts[1].trim() : ""
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
        
        // Get current time in both timezones
        def now = new Date()
        def yourFormat = new SimpleDateFormat("HH:mm")
        def cityFormat = new SimpleDateFormat("HH:mm")
        
        yourFormat.timeZone = TimeZone.getDefault()
        cityFormat.timeZone = cityTimezone
        
        def yourTime = yourFormat.format(now)
        def cityTime = cityFormat.format(now)
        def yourTime12 = convertTo12Hour(yourTime)
        def cityTime12 = convertTo12Hour(cityTime)
        
        // Create three-column layout for current weather
        html.append("<div class='current-weather-container'>")
        
        // Top row: three main columns
        html.append("<div class='current-weather-top'>")
        
        // Column 1: City, Country, Time
        html.append("<div class='location-time-column'>")
        html.append("<div class='location-info'>")
        html.append("<div class='city-name-display'>${cityName}</div>")
        if (countryCode) {
            def countryName = CountryCodeUtil.getCountryName(countryCode)            
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
            def metricValue = roundTemperatureInValue(tempValue)
            def imperialValue = convertTemperatureInValueToImperial(tempValue)
            
            def metricMainTemp = extractMainTemperature(metricValue)
            def metricFeelsLike = extractFeelsLikeTemperature(metricValue)
            def imperialMainTemp = extractMainTemperature(imperialValue)
            def imperialFeelsLike = extractFeelsLikeTemperature(imperialValue)
            
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
            def icon = getWeatherIcon(desc)
            
            html.append("<div class='weather-display'>")
            html.append("<div class='weather-icon-large'>${icon}</div>")
            html.append("<div class='weather-description'>${desc}</div>")
            html.append("</div>")
        }
        html.append("</div>")
        
        html.append("</div>") // End top row
        
        // Bottom section: two columns for additional weather info
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
            html.append("<span class='imperial-units' style='display: none;'>${convertPropertyToImperial('Pressure', weatherData['Pressure'])}</span>")
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
            html.append("<span class='imperial-units' style='display: none;'>${convertPropertyToImperial('Wind Speed', weatherData['Wind Speed'])}</span>")
            html.append("</span>")
            html.append("</div>")
        }
        if (weatherData["Visibility"]) {
            html.append("<div class='weather-detail'>")
            html.append("<span class='detail-label'>Visibility:</span>")
            html.append("<span class='detail-value'>")
            html.append("<span class='metric-units'>${weatherData['Visibility']}</span>")
            html.append("<span class='imperial-units' style='display: none;'>${convertPropertyToImperial('Visibility', weatherData['Visibility'])}</span>")
            html.append("</span>")
            html.append("</div>")
        }
        html.append("</div>")
        
        html.append("</div>") // End bottom row
        html.append("</div>") // End container
    }
    
    return html.toString()
}

def convertTo12Hour(String time24) {
    try {
        def format24 = new SimpleDateFormat("HH:mm")
        def format12 = new SimpleDateFormat("hh:mm a")
        def date = format24.parse(time24)
        def result = format12.format(date)
        return result.toUpperCase() // Capitalize AM/PM
    } catch (Exception e) {
        return time24
    }
}

def convertTemperatureToImperial(String tempStr) {
    def pattern = /(\d+\.?\d*)([°CF]+)/
    def matcher = tempStr =~ pattern
    
    if (matcher) {
        def temp = Double.parseDouble(matcher[0][1])
        def unit = matcher[0][2]
        
        if (unit.contains("C")) {
            // Convert Celsius to Fahrenheit
            def fahrenheit = (temp * 9/5) + 32
            return "${Math.round(fahrenheit)}°F"
        }
    }
    return tempStr // Return original if no conversion needed or already Fahrenheit
}

def convertTemperatureInValueToImperial(String value) {
    // Handle "22°C (feels like 23°C)" format and convert to Fahrenheit
    def pattern = /(\d+\.?\d*)([°CF]+)(\s*\(feels like\s*)(\d+\.?\d*)([°CF]+\))/
    def matcher = value =~ pattern
    
    if (matcher) {
        def temp1 = Double.parseDouble(matcher[0][1])
        def unit1 = matcher[0][2]
        def middleText = matcher[0][3]
        def temp2 = Double.parseDouble(matcher[0][4])
        def unit2 = matcher[0][5]
        
        if (unit1.contains("C")) {
            // Convert to Fahrenheit
            def fahrenheit1 = Math.round((temp1 * 9/5) + 32)
            def fahrenheit2 = Math.round((temp2 * 9/5) + 32)
            return "${fahrenheit1}°F${middleText}${fahrenheit2}°F)"
        }
    }
    return value // Return original if no conversion needed
}

def formatDailyForecastTables(String result, TimeZone cityTimezone, String yourTimezoneName, String cityTimezoneName) {
    def lines = result.split('\n')
    def html = new StringBuilder()
    
    // Add container for side-by-side tables
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
                // Process previous day
                dayForecasts << currentDay
            }
            def dateStr = line.substring(10).trim()
            currentDay = [date: dateStr, forecasts: []]
        } else if (line == "DAY_END") {
            // Day ended, continue to next
        } else if (line.contains(":") && currentDay != null) {
            // Time entry: "1644321600: 15.2°C, light rain" (Unix timestamp)
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
    
    // Add the last day
    if (currentDay != null) {
        dayForecasts << currentDay
    }
    
    // Generate tables for each day
    dayForecasts.each { day ->
        html.append("<div class='daily-table'>")
        
        // Day header
        def date = Date.parse("yyyy-MM-dd", day.date)
        def dayName = date.format("EEEE")
        def displayDate = date.format("MMM dd")
        
        html.append("<h3 class='day-header'>${dayName}<br><span class='date-small'>${displayDate}</span></h3>")
        
        // Table for this day
        html.append("<table class='compact-forecast'>")
        
        day.forecasts.each { forecast ->
            // Convert Unix timestamp to times in both timezones
            def forecastDate = new Date(forecast.timestamp * 1000L)
            
            // Format time in user's timezone
            def userFormat = new SimpleDateFormat("HH:mm")
            userFormat.timeZone = TimeZone.getDefault()
            def userTimeStr = userFormat.format(forecastDate)
            def user12HourStr = convertTo12Hour(userTimeStr)
            
            // Format time in city's timezone
            def cityFormat = new SimpleDateFormat("HH:mm")
            cityFormat.timeZone = cityTimezone
            def cityTimeStr = cityFormat.format(forecastDate)
            def city12HourStr = convertTo12Hour(cityTimeStr)
            
            def tempMetric = roundTemperature(forecast.temp)
            def tempImperial = convertTemperatureToImperial(forecast.temp)
            def weatherIcon = getWeatherIcon(forecast.description)
            
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

def formatLegacyForecastTable(String result, TimeZone cityTimezone, String yourTimezoneName, String cityTimezoneName) {
    // Keep the original forecast table logic for compatibility (implementation omitted for brevity)
    return "<div>Legacy forecast format not implemented in this version</div>"
}

def convertPropertyToImperial(String property, String value) {
    switch (property) {
        case "Pressure":
            // Convert hPa to psi
            def pattern = /(\d+\.?\d*)\s*hPa/
            def matcher = value =~ pattern
            if (matcher) {
                def pressure = Double.parseDouble(matcher[0][1])
                def psi = Math.round(pressure * 0.0145038 * 100) / 100
                return "${psi} psi"
            }
            break
            
        case "Wind Speed":
            // Convert m/s to mph
            def pattern = /(\d+\.?\d*)\s*m\/s/
            def matcher = value =~ pattern
            if (matcher) {
                def speed = Double.parseDouble(matcher[0][1])
                def mph = Math.round(speed * 2.237 * 10) / 10
                return "${mph} mph"
            }
            break
            
        case "Visibility":
            // Convert km to miles
            def pattern = /(\d+\.?\d*)\s*km/
            def matcher = value =~ pattern
            if (matcher) {
                def distance = Double.parseDouble(matcher[0][1])
                def miles = Math.round(distance * 0.621371 * 100) / 100
                return "${miles} miles"
            }
            break
    }
    return value // Return original if no conversion needed
}

def convertTemperatureDisplay(String tempStr, boolean toImperial) {
    def pattern = /(\d+\.?\d*)([°CF]+)/
    def matcher = tempStr =~ pattern
    
    if (matcher) {
        def temp = Double.parseDouble(matcher[0][1])
        def unit = matcher[0][2]
        
        if (toImperial && unit.contains("C")) {
            // Convert Celsius to Fahrenheit
            def fahrenheit = (temp * 9/5) + 32
            return "${Math.round(fahrenheit)}°F"
        } else if (!toImperial && unit.contains("F")) {
            // Convert Fahrenheit to Celsius
            def celsius = (temp - 32) * 5/9
            return "${Math.round(celsius)}°C"
        }
    }
    return roundTemperature(tempStr) // Return rounded original if no conversion needed
}

def convertTemperatureInValue(String value) {
    // Handle "22°C (feels like 23°C)" format and convert to Fahrenheit
    def pattern = /(\d+\.?\d*)([°CF]+)(\s*\(feels like\s*)(\d+\.?\d*)([°CF]+\))/
    def matcher = value =~ pattern
    
    if (matcher) {
        def temp1 = Double.parseDouble(matcher[0][1])
        def unit1 = matcher[0][2]
        def middleText = matcher[0][3]
        def temp2 = Double.parseDouble(matcher[0][4])
        def unit2 = matcher[0][5]
        
        if (unit1.contains("C")) {
            // Convert to Fahrenheit
            def fahrenheit1 = Math.round((temp1 * 9/5) + 32)
            def fahrenheit2 = Math.round((temp2 * 9/5) + 32)
            return "${fahrenheit1}°F${middleText}${fahrenheit2}°F)"
        } else if (unit1.contains("F")) {
            // Convert to Celsius
            def celsius1 = Math.round((temp1 - 32) * 5/9)
            def celsius2 = Math.round((temp2 - 32) * 5/9)
            return "${celsius1}°C${middleText}${celsius2}°C)"
        }
    }
    return value
}

// ...existing helper methods (getCityTimezone, convertTimeToTimezone, etc.)...

def convertTimeToTimezone(String timeStr, TimeZone targetTimezone) {
    try {
        def yourTimezone = TimeZone.getDefault()
        def format = new SimpleDateFormat("HH:mm") // Already uses HH:mm for 24-hour format
        
        format.timeZone = yourTimezone
        def date = format.parse(timeStr)
        
        format.timeZone = targetTimezone
        return format.format(date) // Will maintain HH:mm format
    } catch (Exception e) {
        return timeStr // Return original if conversion fails
    }
}

def convertDateToTimezone(String dateStr, TimeZone targetTimezone) {
    try {
        def yourTimezone = TimeZone.getDefault()
        def format = new SimpleDateFormat("yyyy-MM-dd")
        
        format.timeZone = yourTimezone
        def date = format.parse(dateStr)
        
        format.timeZone = targetTimezone
        return format.format(date)
    } catch (Exception e) {
        return dateStr // Return original if conversion fails
    }
}

// Add weather icon mapping function
def getWeatherIcon(String description) {
    def desc = description.toLowerCase()
    
    // Rain conditions
    if (desc.contains("rain") || desc.contains("drizzle")) {
        if (desc.contains("light")) return "🌦️"
        if (desc.contains("heavy")) return "🌧️"
        return "🌧️"
    }
    
    // Snow conditions
    if (desc.contains("snow")) {
        return "❄️"
    }
    
    // Thunderstorm
    if (desc.contains("thunder") || desc.contains("storm")) {
        return "⛈️"
    }
    
    // Fog/Mist
    if (desc.contains("fog") || desc.contains("mist") || desc.contains("haze")) {
        return "🌫️"
    }
    
    // Clear sky
    if (desc.contains("clear") || desc.contains("sunny")) {
        return "☀️"
    }
    
    // Cloudy conditions
    if (desc.contains("cloud")) {
        if (desc.contains("few") || desc.contains("scattered")) return "🌤️"
        if (desc.contains("broken") || desc.contains("overcast")) return "☁️"
        return "⛅"
    }
    
    // Wind
    if (desc.contains("wind")) {
        return "💨"
    }
    
    // Default for unknown conditions
    return "🌈"
}

def roundTemperature(String tempStr) {
    // Extract number from string like "15.2°C" or "58.4°F"
    def pattern = /(\d+\.?\d*)([°CF]+)/
    def matcher = tempStr =~ pattern
    
    if (matcher) {
        def temp = Double.parseDouble(matcher[0][1])
        def unit = matcher[0][2]
        def rounded = Math.round(temp)
        return "${rounded}${unit}"
    }
    return tempStr // Return original if pattern doesn't match
}

def roundTemperatureInValue(String value) {
    // Handle "22.5°C (feels like 23.1°C)" format
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
    return value // Return original if pattern doesn't match
}

def extractMainTemperature(String value) {
    // Extract just the main temperature from "22°C (feels like 23°C)" format
    def pattern = /(\d+[°CF]+)/
    def matcher = value =~ pattern
    
    if (matcher) {
        return matcher[0][1]
    }
    return value
}

def extractFeelsLikeTemperature(String value) {
    // Extract feels like temperature and format without parentheses
    def pattern = /\(feels like\s*(\d+[°CF]+)\)/
    def matcher = value =~ pattern
    
    if (matcher) {
        return "feels like ${matcher[0][1]}"
    }
    return ""
}

server.start()
println "Server started at http://localhost:8080"
server.join()