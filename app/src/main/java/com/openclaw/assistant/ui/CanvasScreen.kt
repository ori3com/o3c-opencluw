package com.openclaw.assistant.ui

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.openclaw.assistant.node.CanvasController
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun CanvasScreen(
    canvasController: CanvasController,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).also { webView ->
                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }
                webView.webViewClient = CanvasWebViewClient(canvasController)
                canvasController.attach(webView)
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            canvasController.detach()
            webView.destroy()
        }
    )
}

private class CanvasWebViewClient(
    private val canvasController: CanvasController
) : WebViewClient() {

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        canvasController.onPageFinished()
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val token = canvasController.gatewayToken?.takeIf { it.isNotBlank() } ?: return null
        val origin = canvasController.gatewayOrigin ?: return null
        if (!request.url.toString().startsWith(origin)) return null

        return try {
            val conn = URL(request.url.toString()).openConnection() as HttpURLConnection
            request.requestHeaders.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connect()
            WebResourceResponse(
                conn.contentType?.substringBefore(";") ?: "text/html",
                conn.contentEncoding ?: "utf-8",
                conn.inputStream
            )
        } catch (_: Exception) {
            null
        }
    }
}
