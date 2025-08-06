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
def loadHtmlTemplate(String city, String timezone, String units, String timeformat, String currentTableHtml, String forecastTableHtml) {
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
        template = template.replace("{{CURRENT_WEATHER_HEADER}}", '<h2>Current Weather</h2>')
        template = template.replace("{{FORECAST_HEADER}}", '<h2>5-Day Forecast</h2>')
    } else {
        template = template.replace("{{CURRENT_WEATHER_HEADER}}", '')
        template = template.replace("{{FORECAST_HEADER}}", '')
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
        
        if (city && !city.trim().isEmpty()) {
            // Always fetch weather data in metric units - conversion handled client-side
            def weatherClient = new WeatherApiClient(apiKey, false) // Always use metric
            def currentResult = weatherClient.getCurrentWeather(city)
            def forecastResult = weatherClient.getForecast(city)
            def cityTimezone = getCityTimezone(city, apiKey)
            
            // Parse both results to create table HTML - always pass false for useImperial
            currentTableHtml = formatResultAsTable(currentResult, "current", cityTimezone, false)
            forecastTableHtml = formatResultAsTable(forecastResult, "forecast", cityTimezone, false)
        } else {
            // Hide content when no city is provided
            currentTableHtml = ""
            forecastTableHtml = ""
        }
        
        resp.contentType = "text/html"
        def htmlContent = loadHtmlTemplate(city, timezone, units, timeformat, currentTableHtml, forecastTableHtml)
        resp.writer.println(htmlContent)
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
                    
                    // Convert temperature units - generate both metric and imperial
                    def tempMetric = roundTemperature(tempRaw)
                    def tempImperial = convertTemperatureToImperial(tempRaw)
                    
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
                    html.append("<span class='metric-units'>${tempMetric}</span>")
                    html.append("<span class='imperial-units' style='display: none;'>${tempImperial}</span>")
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
                        def imperialValue = convertTemperatureInValueToImperial(value)
                        
                        html.append("<tr>")
                        html.append("<td>${property}</td>")
                        html.append("<td>")
                        html.append("<span class='metric-units'>${metricValue}</span>")
                        html.append("<span class='imperial-units' style='display: none;'>${imperialValue}</span>")
                        html.append("</td>")
                        html.append("</tr>")
                    } else {
                        // Handle other properties that may have units
                        if (property == "Pressure" || property == "Wind Speed" || property == "Visibility") {
                            def metricValue = value
                            def imperialValue = convertPropertyToImperial(property, value)
                            
                            html.append("<tr>")
                            html.append("<td>${property}</td>")
                            html.append("<td>")
                            html.append("<span class='metric-units'>${metricValue}</span>")
                            html.append("<span class='imperial-units' style='display: none;'>${imperialValue}</span>")
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

server.start()
println "Server started at http://localhost:8080"
server.join()