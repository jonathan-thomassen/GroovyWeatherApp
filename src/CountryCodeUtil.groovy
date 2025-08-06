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
