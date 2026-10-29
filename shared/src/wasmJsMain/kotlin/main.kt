import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.bindToBrowserNavigation
import com.example.condominio.App
import com.example.condominio.data.local.LocalStorageTokenManager
import com.example.condominio.data.local.TokenManager
import com.example.condominio.data.utils.PlatformFileReader
import com.example.condominio.data.utils.WebFileReader
import com.example.condominio.di.sharedModule
import com.example.condominio.ui.screens.register.extractBuildingCode
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js
import kotlinx.browser.document
import kotlinx.browser.window
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.w3c.dom.url.URLSearchParams

private val webModule = module {
    single<HttpClientEngine> { Js.create() }
    single<TokenManager> { LocalStorageTokenManager() }
    single<PlatformFileReader> { WebFileReader() }
}

// Browser tab titles per route prefix.
private val pageTitles = linkedMapOf(
    "dashboard" to "Inicio", "invoice" to "Pagos", "payment_history" to "Historial de pagos",
    "payment_detail" to "Detalle de pago", "create_payment" to "Registrar pago", "billboard" to "Cartelera",
    "decision" to "Votaciones", "profile" to "Perfil", "edit_profile" to "Editar perfil",
    "change_password" to "Cambiar contraseña", "notification_settings" to "Notificaciones",
    "login" to "Iniciar sesión", "register" to "Registro", "forgot_password" to "Recuperar contraseña",
)

@OptIn(ExperimentalComposeUiApi::class, ExperimentalBrowserHistoryApi::class)
fun main() {
    startKoin { modules(sharedModule, webModule) }

    // Building QR codes encoding `https://<app>/?code=COND-XXXX` open straight into registration.
    val registerCode = URLSearchParams(window.location.search.toJsString()).get("code")
        ?.let(::extractBuildingCode)?.takeIf { it.isNotBlank() }

    ComposeViewport(document.body!!) {
        App(registerCode = registerCode, onNavHostReady = { nav ->
            nav.addOnDestinationChangedListener { _, destination, _ ->
                val section = pageTitles.entries.firstOrNull { destination.route?.startsWith(it.key) == true }?.value
                document.title = if (section != null) "$section · Apto" else "Apto Condominios"
            }
            nav.bindToBrowserNavigation()
        })
    }
    document.getElementById("loading")?.remove()
}
