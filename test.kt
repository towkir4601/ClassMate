import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun main() {
    val time = LocalTime.parse("14:10")
    val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    println(time.format(formatter))
}
