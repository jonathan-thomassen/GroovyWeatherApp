@Grab('org.eclipse.jetty:jetty-server:9.4.48.v20220622')
@Grab('org.eclipse.jetty:jetty-servlet:9.4.48.v20220622')

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import javax.servlet.http.*
import java.text.SimpleDateFormat
import java.util.TimeZone

// Load the separated classes using proper class loading
def shell = new GroovyShell()
shell.evaluate(new File("CountryCodeUtil.groovy"))
shell.evaluate(new File("TemperatureConverter.groovy"))
shell.evaluate(new File("WeatherApiClient.groovy"))
shell.evaluate(new File("WeatherFormatter.groovy"))

/**
 * Main web server class - simplified and focused on server setup and request handling
 */
class WeatherWebServer {
    
    private final String apiKey
    private final int port
    private final WeatherApiClient weatherClient
    
    WeatherWebServer(String apiKey, int port = 8080) {
        this.apiKey = apiKey
        this.port = port
        this.weatherClient = new WeatherApiClient(apiKey)
    }
    
    /**
     * Start the web server
     */
    void start() {
        def server = new Server(port)
        def context = new ServletContextHandler()
        context.contextPath = "/"
        server.handler = context

        // Add static file serving
        context.addServlet(new ServletHolder(createStaticFileServlet()), "/static/*")
        
        // Add main request handler
        context.addServlet(new ServletHolder(createMainServlet()), "/*")

        server.start()
        println "Server started at http://localhost:${port}"
        server.join()
    }
    
    /**
     * Create servlet for static file serving
     */
    private HttpServlet createStaticFileServlet() {
        return new HttpServlet() {
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
        }
    }
    
    /**
     * Create main servlet for weather requests
     */
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
                    
                    // Check if current weather failed - if so, don't try forecast
                    if (WeatherFormatter.isErrorResult(currentResult)) {
                        // Only show one error message for both current and forecast
                        currentTableHtml = WeatherFormatter.formatErrorAsHtml(currentResult)
                        forecastTableHtml = "" // Don't show forecast error if current weather failed
                    } else {
                        // Current weather succeeded, try forecast
                        def forecastResult = weatherClient.getForecast(city)
                        cityTimezone = weatherClient.getCityTimezone(city)
                        
                        // Extract city name from current weather result
                        cityName = WeatherFormatter.extractCityName(currentResult)
                        
                        // Format both results
                        currentTableHtml = formatResultAsTable(currentResult, "current", cityTimezone)
                        
                        // Only show forecast if it succeeded, otherwise show a simplified message
                        if (WeatherFormatter.isErrorResult(forecastResult)) {
                            forecastTableHtml = "<div class='forecast-error-notice'>Forecast data is temporarily unavailable for this location.</div>"
                        } else {
                            forecastTableHtml = formatResultAsTable(forecastResult, "forecast", cityTimezone)
                        }
                    }
                } else {
                    // Hide content when no city is provided
                    currentTableHtml = ""
                    forecastTableHtml = ""
                }
                
                resp.contentType = "text/html"
                def htmlContent = loadHtmlTemplate(city, timezone, units, timeformat, currentTableHtml, forecastTableHtml, cityName, cityTimezone)
                resp.writer.println(htmlContent)
            }
        }
    }
    
    /**
     * Load and process HTML template
     */
    private String loadHtmlTemplate(String city, String timezone, String units, String timeformat, 
                                  String currentTableHtml, String forecastTableHtml, String cityName, TimeZone cityTimezone) {
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
        template = template.replace("{{CITY_HEADER}}", '')
        template = template.replace("{{CURRENT_WEATHER_CONTENT}}", currentTableHtml)
        template = template.replace("{{FORECAST_CONTENT}}", forecastTableHtml)
        
        return template
    }
    
    /**
     * Format weather result as HTML table
     */
    private String formatResultAsTable(String result, String type, TimeZone cityTimezone) {
        // Handle errors first
        if (WeatherFormatter.isErrorResult(result)) {
            return WeatherFormatter.formatErrorAsHtml(result)
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
            }
        } else {
            // Current weather formatting
            return formatCurrentWeatherHtml(lines, cityTimezone, yourTimezoneName, cityTimezoneName)
        }
        
        return html.toString()
    }
    
    /**
     * Format current weather as HTML
     */
    private String formatCurrentWeatherHtml(def lines, TimeZone cityTimezone, String yourTimezoneName, String cityTimezoneName) {
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
        def yourTime12 = WeatherFormatter.convertTo12Hour(yourTime)
        def cityTime12 = WeatherFormatter.convertTo12Hour(cityTime)
        
        def html = new StringBuilder()
        
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
    
    /**
     * Format daily forecast tables
     */
    private String formatDailyForecastTables(String result, TimeZone cityTimezone, String yourTimezoneName, String cityTimezoneName) {
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
                def user12HourStr = WeatherFormatter.convertTo12Hour(userTimeStr)
                
                // Format time in city's timezone
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

// Start the server
def server = new WeatherWebServer(apiKey)
server.start()
