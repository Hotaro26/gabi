import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars

fun test() {
    val insets = WindowInsets.systemBars.only(WindowInsetsSides.Top)
}
