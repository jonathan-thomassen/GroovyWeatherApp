/**
 * Utility class for temperature conversion and formatting
 * Consolidates all temperature-related operations
 */
class TemperatureConverter {
    
    /**
     * Convert a single temperature value from Celsius to Fahrenheit
     * Handles formats like "22°C" or "58.4°F"
     */
    static String convertToFahrenheit(String tempStr) {
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
    
    /**
     * Convert a single temperature value from Fahrenheit to Celsius
     */
    static String convertToCelsius(String tempStr) {
        def pattern = /(\d+\.?\d*)([°CF]+)/
        def matcher = tempStr =~ pattern
        
        if (matcher) {
            def temp = Double.parseDouble(matcher[0][1])
            def unit = matcher[0][2]
            
            if (unit.contains("F")) {
                // Convert Fahrenheit to Celsius
                def celsius = (temp - 32) * 5/9
                return "${Math.round(celsius)}°C"
            }
        }
        return tempStr // Return original if no conversion needed or already Celsius
    }
    
    /**
     * Convert temperature with feels-like format
     * Handles "22°C (feels like 23°C)" format
     */
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
                // Convert to Fahrenheit
                def fahrenheit1 = Math.round((temp1 * 9/5) + 32)
                def fahrenheit2 = Math.round((temp2 * 9/5) + 32)
                return "${fahrenheit1}°F${middleText}${fahrenheit2}°F)"
            } else if (!toFahrenheit && unit1.contains("F")) {
                // Convert to Celsius
                def celsius1 = Math.round((temp1 - 32) * 5/9)
                def celsius2 = Math.round((temp2 - 32) * 5/9)
                return "${celsius1}°C${middleText}${celsius2}°C)"
            }
        }
        return value // Return original if no conversion needed
    }
    
    /**
     * Round temperature values to nearest integer
     * Handles formats like "15.2°C" or "58.4°F"
     */
    static String roundTemperature(String tempStr) {
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
    
    /**
     * Round temperature with feels-like format
     * Handles "22.5°C (feels like 23.1°C)" format
     */
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
        return value // Return original if pattern doesn't match
    }
    
    /**
     * Extract just the main temperature from "22°C (feels like 23°C)" format
     */
    static String extractMainTemperature(String value) {
        def pattern = /(\d+[°CF]+)/
        def matcher = value =~ pattern
        
        if (matcher) {
            return matcher[0][1]
        }
        return value
    }
    
    /**
     * Extract feels like temperature and format without parentheses
     */
    static String extractFeelsLikeTemperature(String value) {
        def pattern = /\(feels like\s*(\d+[°CF]+)\)/
        def matcher = value =~ pattern
        
        if (matcher) {
            return "feels like ${matcher[0][1]}"
        }
        return ""
    }
}
