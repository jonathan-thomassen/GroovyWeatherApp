@Grab('org.apache.httpcomponents:httpclient:4.5.14')
@Grab('org.apache.httpcomponents:httpcore:4.4.16')
@Grab('com.fasterxml.jackson.core:jackson-databind:2.15.2')

import groovy.json.JsonSlurper
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.net.URLEncoder

/**
 * Weather API Client using OpenWeatherMap API
 * Handles all external API communication
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
        def httpClient = HttpClients.createDefault()
        try {
            def encodedCity = URLEncoder.encode(city, "UTF-8")
            def url = "${baseUrl}/weather?q=${encodedCity}&appid=${apiKey}&units=metric"
            
            def httpGet = new HttpGet(url)
            def response = httpClient.execute(httpGet)
            def responseBody = EntityUtils.toString(response.entity)
            
            if (response.statusLine.statusCode == 200) {
                def weatherData = jsonSlurper.parseText(responseBody)
                return formatCurrentWeather(weatherData)
            } else {
                return handleApiError(response.statusLine.statusCode, city)
            }
            
        } catch (Exception e) {
            return "CONNECTION_ERROR: Unable to connect to weather service. Please check your internet connection and try again."
        } finally {
            httpClient.close()
        }
    }
    
    /**
     * Get 5-day weather forecast for a city
     */
    def getForecast(String city) {
        def httpClient = HttpClients.createDefault()
        try {
            def encodedCity = URLEncoder.encode(city, "UTF-8")
            def url = "${baseUrl}/forecast?q=${encodedCity}&appid=${apiKey}&units=metric"
            
            def httpGet = new HttpGet(url)
            def response = httpClient.execute(httpGet)
            def responseBody = EntityUtils.toString(response.entity)
            
            if (response.statusLine.statusCode == 200) {
                def forecastData = jsonSlurper.parseText(responseBody)
                return formatForecastAsDaily(forecastData)
            } else {
                return handleApiError(response.statusLine.statusCode, city)
            }
            
        } catch (Exception e) {
            return "CONNECTION_ERROR: Unable to connect to weather service. Please check your internet connection and try again."
        } finally {
            httpClient.close()
        }
    }
    
    /**
     * Get timezone information for a city
     */
    def getCityTimezone(String city) {
        def httpClient = HttpClients.createDefault()
        try {
            def encodedCity = URLEncoder.encode(city, "UTF-8")
            def url = "${baseUrl}/weather?q=${encodedCity}&appid=${apiKey}"
            
            def httpGet = new HttpGet(url)
            def response = httpClient.execute(httpGet)
            def responseBody = EntityUtils.toString(response.entity)
            
            if (response.statusLine.statusCode == 200) {
                def weatherData = jsonSlurper.parseText(responseBody)
                def timezoneOffset = weatherData.timezone as Integer
                
                // Create a custom timezone based on the offset
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
        
        return TimeZone.getDefault() // Fallback to system timezone
    }
    
    /**
     * Handle API error responses with appropriate messages
     */
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
                result << "${forecast.dt}: ${forecast.main.temp}°C, ${forecast.weather[0].description}"
            }
            result << "DAY_END"
        }
        
        result << "DAILY_FORECAST_END"
        return result.join("\n")
    }
}
