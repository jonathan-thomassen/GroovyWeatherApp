import java.text.SimpleDateFormat
import java.util.TimeZone

/**
 * Handles all weather data formatting and HTML generation
 * Separates presentation logic from business logic
 */
class WeatherFormatter {
    
    /**
     * Check if result is an error message
     */
    static boolean isErrorResult(String result) {
        return result.startsWith("CITY_NOT_FOUND:") || result.startsWith("API_ERROR:") || 
               result.startsWith("RATE_LIMIT:") || result.startsWith("SERVICE_ERROR:") || 
               result.startsWith("CONNECTION_ERROR:") || result.startsWith("Error:")
    }
    
    /**
     * Extract city name from weather result
     */
    static String extractCityName(String result) {
        // Handle error formats
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
    
    /**
     * Convert time string to 12-hour format
     */
    static String convertTo12Hour(String time24) {
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
    
    /**
     * Get weather icon based on description
     */
    static String getWeatherIcon(String description) {
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
    
    /**
     * Convert property values to imperial units
     */
    static String convertPropertyToImperial(String property, String value) {
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
    
    /**
     * Format error messages as HTML
     */
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
            // Fallback for old-style errors
            def message = result.substring(6).trim()
            return "<div class='error error-general'><div class='error-icon'>⚠️</div><div class='error-content'><h3>Error</h3><p>${message}</p></div></div>"
        }
        return result
    }
}
