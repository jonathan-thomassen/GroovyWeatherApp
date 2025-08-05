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

static void main(String[] args) {
    def apiKeyFile = new File("key")
    
    if (!apiKeyFile.exists()) {
        println "Error: 'key' file not found in current directory"
        println "Please create a file named 'key' containing your OpenWeatherMap API key"
        println "To get an API key, sign up at: https://openweathermap.org/api"
        return
    }
    
    def apiKey = apiKeyFile.text.trim()
    
    if (apiKey.isEmpty()) {
        println "Error: 'key' file is empty"
        println "Please add your OpenWeatherMap API key to the 'key' file"
        return
    }
    
    if (!apiKey.matches("[a-zA-Z0-9]{32}")) {
        println "Error: API key must be 32 characters long and alphanumeric"
        println "Please check your API key in the 'key' file"
        return
    }
    
    def weatherClient = new WeatherApiClient(apiKey)
    
    println "Weather Forecast API Client"
    println "======================="
    println ""
    
    def scanner = new Scanner(System.in)
    while (true) {
        println "Enter a city name (or 'quit' to exit):"
        def input = scanner.nextLine().trim()
        
        if (input.toLowerCase() == 'quit') {
            break
        }
        
        if (input) {
            println ""
            println "Choose option:"
            println "1. Current weather"
            println "2. 5-day forecast"
            def choice = scanner.nextLine().trim()
            
            println ""
            if (choice == "1") {
                println weatherClient.getCurrentWeather(input)
            } else if (choice == "2") {
                println weatherClient.getForecast(input)
            } else {
                println "Invalid choice. Showing current weather:"
                println weatherClient.getCurrentWeather(input)
            }
            println ""
        }
    }
    
    println "Thank you for using the Weather API Client!"
}
