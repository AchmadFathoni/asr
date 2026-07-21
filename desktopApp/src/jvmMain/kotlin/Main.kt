import androidx.compose.ui.window.application
import androidx.compose.ui.window.Window
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import com.asr.desktop.DesktopAppModule
import com.asr.ui.app.MainApp
import org.koin.plugin.module.dsl.startKoin

fun main() {
    startKoin<DesktopAppModule> {
        allowOverride(true)
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "ASR",
            state = WindowState(width = 480.dp, height = 800.dp),
        ) {
            MainApp()
        }
    }
}
