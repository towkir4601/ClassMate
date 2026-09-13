import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun main() {
    val targetDateString = LocalDate.now().plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    println("String: $targetDateString")
    try {
        val parsed = LocalDate.parse(targetDateString)
        println("Parsed: $parsed")
        println("IsBefore: ${parsed.isBefore(LocalDate.now())}")
    } catch (e: Exception) {
        println("Exception: ${e.message}")
    }
}
