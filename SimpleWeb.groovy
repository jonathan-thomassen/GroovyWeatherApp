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
 * Weather API Client using OpenWeatherMap API
 */
class WeatherApiClient {
    
    private final String apiKey
    private final String baseUrl = "https://api.openweathermap.org/data/2.5"
    private final JsonSlurper jsonSlurper = new JsonSlurper()
    private final String units
    private final String tempUnit
    private final String speedUnit
    private final boolean useImperialUnits
    
    WeatherApiClient(String apiKey, boolean useImperialUnits = false) {
        this.apiKey = apiKey
        this.units = useImperialUnits ? "imperial" : "metric"
        this.tempUnit = useImperialUnits ? "°F" : "°C"
        this.speedUnit = useImperialUnits ? "mph" : "m/s"
        this.useImperialUnits = useImperialUnits
    }
    
    /**
     * Get current weather for a city
     */
    def getCurrentWeather(String city) {
        try {
            def encodedCity = URLEncoder.encode(city, "UTF-8")
            def url = "${baseUrl}/weather?q=${encodedCity}&appid=${apiKey}&units=${units}"
            
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
            def url = "${baseUrl}/forecast?q=${encodedCity}&appid=${apiKey}&units=${units}"
            
            def httpClient = HttpClients.createDefault()
            def httpGet = new HttpGet(url)
            
            def response = httpClient.execute(httpGet)
            def responseBody = EntityUtils.toString(response.entity)
            
            if (response.statusLine.statusCode == 200) {
                def forecastData = jsonSlurper.parseText(responseBody)
                return formatForecast(forecastData)
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
        result << "Temperature: ${weatherData.main.temp}${tempUnit} (feels like ${weatherData.main.feels_like}${tempUnit})"
        result << "Weather: ${weatherData.weather[0].main} - ${weatherData.weather[0].description}"
        result << "Humidity: ${weatherData.main.humidity}%"
        
        // Convert pressure: hPa to psi if imperial units
        if (useImperialUnits) {
            def pressurePsi = Math.round(weatherData.main.pressure * 0.0145038 * 100) / 100
            result << "Pressure: ${pressurePsi} psi"
        } else {
            result << "Pressure: ${weatherData.main.pressure} hPa"
        }
        
        result << "Wind Speed: ${weatherData.wind.speed} ${speedUnit}"
        
        // Convert visibility: meters to miles if imperial units
        if (useImperialUnits) {
            def visibilityMiles = Math.round(weatherData.visibility * 0.000621371 * 100) / 100
            result << "Visibility: ${visibilityMiles} miles"
        } else {
            result << "Visibility: ${weatherData.visibility / 1000} km"
        }
        
        result << ""
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
            
            result << "${timeStr}: ${forecast.main.temp}${tempUnit}, ${forecast.weather[0].description}"
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

context.addServlet(new ServletHolder(new HttpServlet() {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        resp.contentType = "text/html"
        resp.writer.println("""
            <html>
            <head>
                <title>Volborg Weather</title>
                <style>
                    body { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; }
                    .form-group { margin: 15px 0; }
                    input, select, button { padding: 10px; margin: 5px; font-size: 16px; }
                    button { background: #007cba; color: white; border: none; cursor: pointer; }
                    button:hover { background: #005a87; }
                </style>
            </head>
            <body>
                <h1>Volborg Weather</h1>
                <form method="post">
                    <div class="form-group">
                        <label>City:</label><br>
                        <input type="text" name="city" required placeholder="Enter city name">
                    </div>
                    <div class="form-group">
                        <label>Type:</label><br>
                        <select name="type">
                            <option value="current">Current Weather</option>
                            <option value="forecast">5-Day Forecast</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Units:</label><br>
                        <select name="units">
                            <option value="metric">Metric (°C, m/s)</option>
                            <option value="imperial">Imperial (°F, mph)</option>
                        </select>
                    </div>
                    <button type="submit">Get Weather</button>
                </form>
            </body>
            </html>
        """)
    }
    
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        def city = req.getParameter("city")
        def type = req.getParameter("type")
        def useImperial = req.getParameter("units") == "imperial"
        
        def weatherClient = new WeatherApiClient(apiKey, useImperial)
        def result
        def cityTimezone = null
        
        if (type == "forecast") {
            result = weatherClient.getForecast(city)
            cityTimezone = getCityTimezone(city, apiKey)
        } else {
            result = weatherClient.getCurrentWeather(city)
            cityTimezone = getCityTimezone(city, apiKey)
        }
        
        // Parse the result to create table HTML
        def tableHtml = formatResultAsTable(result, type, cityTimezone, useImperial)
        
        resp.contentType = "text/html"
        resp.writer.println("""
            <html>
            <head>
                <title>Volborg Weather</title>
                <style>
                    body { font-family: Arial, sans-serif; max-width: 900px; margin: 0 auto; padding: 20px; }
                    h1 { color: #333; }
                    table { width: 100%; border-collapse: collapse; margin: 20px 0; }
                    th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
                    th { background-color: #007cba; color: white; }
                    tr:hover { background-color: #f5f5f5; }
                    .forecast-date { background-color: #e6f3ff; font-weight: bold; }
                    a { color: #007cba; text-decoration: none; padding: 10px 15px; background: #f0f0f0; border-radius: 5px; display: inline-block; margin-top: 20px; }
                    a:hover { text-decoration: underline; background: #e0e0e0; }
                    .error { color: red; background: #ffe6e6; padding: 15px; border-radius: 5px; }
                    .toggle-section { margin: 20px 0; padding: 15px; background: #f8f9fa; border-radius: 8px; }
                    .toggle-group { margin: 10px 0; }
                    .toggle-button { background: #007cba; color: white; border: none; padding: 8px 16px; margin: 0 5px; cursor: pointer; border-radius: 4px; }
                    .toggle-button.active { background: #005a87; }
                    .toggle-button:hover { background: #005a87; }
                    h3 { margin: 5px 0; }
                </style>
                <script>
                    function toggleTimezone(showLocal) {
                        const localElements = document.querySelectorAll('.local-time');
                        const yourElements = document.querySelectorAll('.your-time');
                        const localBtn = document.getElementById('local-btn');
                        const yourBtn = document.getElementById('your-btn');
                        
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
                </script>
            </head>
            <body>
                <h1>Volborg Weather</h1>
                <div class="toggle-section">
                    <div class="toggle-group">
                        <h3>Timezone:</h3>
                        <button id="your-btn" class="toggle-button active" onclick="toggleTimezone(false)">Your Timezone</button>
                        <button id="local-btn" class="toggle-button" onclick="toggleTimezone(true)">Local Timezone</button>
                    </div>
                    
                    <div class="toggle-group">
                        <h3>Units:</h3>
                        <button id="metric-btn" class="toggle-button ${useImperial ? '' : 'active'}" onclick="toggleUnits(false)">Metric (°C)</button>
                        <button id="imperial-btn" class="toggle-button ${useImperial ? 'active' : ''}" onclick="toggleUnits(true)">Imperial (°F)</button>
                    </div>
                    
                    <div class="toggle-group">
                        <h3>Time Format:</h3>
                        <button id="time24-btn" class="toggle-button active" onclick="toggleTimeFormat(false)">24 Hour</button>
                        <button id="time12-btn" class="toggle-button" onclick="toggleTimeFormat(true)">12 Hour</button>
                    </div>
                </div>
                ${tableHtml}
                <a href="/">← Back to search</a>
            </body>
            </html>
        """)
    }
}), "/*")

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
        // Format forecast as table with date subheadings
        def currentDate = ""
        def tableOpen = false
        def cityName = ""
        
        // Extract city name from the first line
        lines.each { line ->
            if (line.startsWith("===") && line.contains("Forecast for")) {
                def match = line =~ /=== 5-Day Forecast for (.+?) ===/
                if (match) {
                    cityName = match[0][1]
                }
            }
        }
        
        lines.each { line ->
            line = line.trim()
            if (line.startsWith("===") || line.isEmpty()) {
                // Skip header and empty lines
            } else if (line.startsWith("---")) {
                // Close previous table if open
                if (tableOpen) {
                    html.append("</table>")
                    tableOpen = false
                }
                
                // Date header - create subheading with both timezones
                def dateStr = line.replaceAll("---", "").trim()
                def localDateStr = convertDateToTimezone(dateStr, cityTimezone)
                
                html.append("<h3 style='color: #007cba; margin-top: 30px; margin-bottom: 10px; border-bottom: 2px solid #007cba; padding-bottom: 5px;'>")
                html.append("<span class='your-time'>${dateStr}</span>")
                html.append("<span class='local-time' style='display: none;'>${localDateStr}</span>")
                html.append("</h3>")
                
                // Start new table for this date with timezone-aware headers
                html.append("<table>")
                html.append("<tr>")
                html.append("<th>")
                html.append("<span class='your-time'>Time (${yourTimezoneName})</span>")
                html.append("<span class='local-time' style='display: none;'>Time (${cityTimezoneName})</span>")
                html.append("</th>")
                html.append("<th>Temperature</th>")
                html.append("<th>Weather</th>")
                html.append("</tr>")
                tableOpen = true
                
            } else if (line.contains(":") && !line.startsWith("===")) {
                // Time entry: "09:00: 15.2°C, light rain"
                def timePattern = /^(\d{2}:\d{2}):\s*(.+)$/
                def matcher = line =~ timePattern
                
                if (matcher && tableOpen) {
                    def timeStr = matcher[0][1]  // "09:00"
                    def weatherInfo = matcher[0][2]  // "15.2°C, light rain"
                    def tempAndDesc = weatherInfo.split(",", 2)
                    def tempRaw = tempAndDesc.length > 0 ? tempAndDesc[0].trim() : ""
                    def desc = tempAndDesc.length > 1 ? tempAndDesc[1].trim() : ""
                    
                    // Convert time to city timezone and formats
                    def localTimeStr = convertTimeToTimezone(timeStr, cityTimezone)
                    def time12Your = convertTo12Hour(timeStr)
                    def time12Local = convertTo12Hour(localTimeStr)
                    
                    // Convert temperature units
                    def tempMetric = roundTemperature(tempRaw)
                    def tempImperial = convertTemperatureDisplay(tempRaw, true)
                    
                    // Convert description to weather icon
                    def weatherIcon = getWeatherIcon(desc)
                    
                    html.append("<tr>")
                    html.append("<td>")
                    // Your timezone times
                    html.append("<span class='your-time'>")
                    html.append("<span class='time-24h'>${timeStr}</span>")
                    html.append("<span class='time-12h' style='display: none;'>${time12Your}</span>")
                    html.append("</span>")
                    // Local timezone times
                    html.append("<span class='local-time' style='display: none;'>")
                    html.append("<span class='time-24h'>${localTimeStr}</span>")
                    html.append("<span class='time-12h' style='display: none;'>${time12Local}</span>")
                    html.append("</span>")
                    html.append("</td>")
                    html.append("<td>")
                    html.append("<span class='metric-units' style='display: ${useImperial ? 'none' : 'inline'};'>${tempMetric}</span>")
                    html.append("<span class='imperial-units' style='display: ${useImperial ? 'inline' : 'none'};'>${tempImperial}</span>")
                    html.append("</td>")
                    html.append("<td style='font-size: 24px; text-align: center;'>${weatherIcon}</td>")
                    html.append("</tr>")
                }
            }
        }
        
        // Close final table if open
        if (tableOpen) {
            html.append("</table>")
        }
        
    } else {
        // Format current weather as table
        html.append("<table>")
        html.append("<tr><th>Property</th><th>Value</th></tr>")
        
        lines.each { line ->
            line = line.trim()
            if (line.startsWith("===")) {
                // City header - extract city name
                def cityName = line.replaceAll("===", "").replaceAll("Current Weather for", "").trim()
                html.append("<tr class='forecast-date'><td colspan='2'><strong>${cityName}</strong></td></tr>")
            } else if (line.contains(":") && !line.isEmpty()) {
                // Property line: "Temperature: 22.5°C (feels like 23.1°C)"
                def parts = line.split(":", 2)
                if (parts.length == 2) {
                    def property = parts[0].trim()
                    def value = parts[1].trim()
                    
                    // Handle temperature with unit conversion
                    if (property == "Temperature") {
                        def metricValue = roundTemperatureInValue(value)
                        def imperialValue = convertTemperatureInValue(value)
                        
                        html.append("<tr>")
                        html.append("<td>${property}</td>")
                        html.append("<td>")
                        html.append("<span class='metric-units' style='display: ${useImperial ? 'none' : 'inline'};'>${metricValue}</span>")
                        html.append("<span class='imperial-units' style='display: ${useImperial ? 'inline' : 'none'};'>${imperialValue}</span>")
                        html.append("</td>")
                        html.append("</tr>")
                    } else {
                        // Convert weather descriptions to icons
                        if (property == "Weather") {
                            def descParts = value.split(" - ", 2)
                            def desc = descParts.length > 1 ? descParts[1] : value
                            def icon = getWeatherIcon(desc)
                            value = "${icon} ${value}"
                        }
                        
                        html.append("<tr>")
                        html.append("<td>${property}</td>")
                        html.append("<td>${value}</td>")
                        html.append("</tr>")
                    }
                }
            }
        }
        html.append("</table>")
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

server.start()
println "Server started at http://localhost:8080"
server.join()