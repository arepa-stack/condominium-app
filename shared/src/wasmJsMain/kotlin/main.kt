import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.bindToBrowserNavigation
import com.example.condominio.App
import com.example.condominio.data.local.LocalStorageTokenManager
import com.example.condominio.data.local.TokenManager
import com.example.condominio.data.model.Payment
import com.example.condominio.data.utils.PdfService
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
    // ponytail: no receipt PDF on web (Android generates it but never shows it either).
    single<PdfService> { object : PdfService { override fun generateReceipt(payment: Payment) = "" } }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalBrowserHistoryApi::class)
fun main() {
    startKoin { modules(sharedModule, webModule) }

    // Building QR codes encoding `https://<app>/?code=COND-XXXX` open straight into registration.
    val registerCode = URLSearchParams(window.location.search.toJsString()).get("code")
        ?.let(::extractBuildingCode)?.takeIf { it.isNotBlank() }

    ComposeViewport(document.body!!) {
        App(registerCode = registerCode, onNavHostReady = { it.bindToBrowserNavigation() })
    }
    document.getElementById("loading")?.remove()
}
