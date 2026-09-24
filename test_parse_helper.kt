import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun parseTimeSafe(timeStr: String): LocalTime? {
    return try {
        LocalTime.parse(timeStr) // Try ISO format first (e.g., "09:00" or "14:30")
    } catch (e: Exception) {
        try {
            // Try 12-hour format with AM/PM
            val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
            LocalTime.parse(timeStr.uppercase(), formatter)
        } catch (e2: Exception) {
            try {
                // Try 12-hour format with two digit hour
                val formatter2 = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
                LocalTime.parse(timeStr.uppercase(), formatter2)
            } catch (e3: Exception) {
                println("Failed to parse: $timeStr")
                null
            }
        }
    }
}

fun main() {
    println(parseTimeSafe("09:00"))
    println(parseTimeSafe("09:00 AM"))
    println(parseTimeSafe("9:00 AM"))
    println(parseTimeSafe("02:40 PM"))
    println(parseTimeSafe("14:40"))
}
