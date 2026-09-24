import java.time.LocalTime
import java.time.format.DateTimeFormatter

fun main() {
    try {
        println(LocalTime.parse("09:00 AM"))
    } catch (e: Exception) {
        println("Error 1: ${e.message}")
    }
    
    try {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        println(LocalTime.parse("09:00 AM", formatter))
    } catch (e: Exception) {
        println("Error 2: ${e.message}")
    }
}
