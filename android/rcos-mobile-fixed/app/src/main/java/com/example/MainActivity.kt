package com.example

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.InputStream

class MainActivity : Activity() {
    private lateinit var webView: WebView

    companion object {
        private const val TAG = "RCOS_WEBVIEW"
        private const val ASSET_HOST = "appassets.androidplatform.net"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set native window decor to dark theme immediately
        window.decorView.setBackgroundColor(Color.parseColor("#060B08"))

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Ensure WebView background matches theme so it never flashes white
            setBackgroundColor(Color.parseColor("#060B08"))

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val uri = request?.url ?: return null
                    if (uri.scheme.equals("https", ignoreCase = true) && uri.host.equals(ASSET_HOST, ignoreCase = true)) {
                        return handleAssetRequest(uri)
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    Log.e(TAG, "Asset/Network error on ${request?.url}: ${error?.description} (code: ${error?.errorCode})")
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    consoleMessage?.let {
                        val level = when (it.messageLevel()) {
                            ConsoleMessage.MessageLevel.ERROR -> Log.ERROR
                            ConsoleMessage.MessageLevel.WARNING -> Log.WARN
                            else -> Log.DEBUG
                        }
                        Log.println(level, TAG, "[JS] ${it.message()} (${it.sourceId()}:${it.lineNumber()})")
                    }
                    return true
                }
            }

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
                javaScriptCanOpenWindowsAutomatically = true
            }
        }

        setContentView(webView)

        // Load via secure virtual HTTPS host to allow ES Modules, CORS, and modern web APIs
        webView.loadUrl("https://$ASSET_HOST/index.html")
    }

    private fun handleAssetRequest(uri: Uri): WebResourceResponse? {
        val rawPath = uri.path?.removePrefix("/") ?: ""
        val targetPath = when {
            rawPath.isEmpty() || rawPath == "/" || rawPath == "index.html" -> "index.html"
            rawPath.startsWith("assets/assets/") -> rawPath.removePrefix("assets/")
            else -> rawPath
        }

        // Attempt resolving asset stream
        val assetStream = openAssetStream(targetPath)
            ?: (if (targetPath.startsWith("assets/")) openAssetStream(targetPath.removePrefix("assets/")) else null)
            ?: (if (!targetPath.startsWith("assets/")) openAssetStream("assets/$targetPath") else null)

        if (assetStream == null) {
            Log.w(TAG, "Asset not found in bundle: $rawPath -> $targetPath")
            return null
        }

        val mimeType = getMimeType(targetPath)
        val response = WebResourceResponse(mimeType, "UTF-8", assetStream)
        response.responseHeaders = mapOf(
            "Access-Control-Allow-Origin" to "*",
            "Access-Control-Allow-Methods" to "GET, POST, OPTIONS",
            "Access-Control-Allow-Headers" to "*",
            "Cache-Control" to "no-cache"
        )
        return response
    }

    private fun openAssetStream(path: String): InputStream? {
        return try {
            assets.open(path)
        } catch (e: Exception) {
            null
        }
    }

    private fun getMimeType(path: String): String {
        val lower = path.lowercase()
        return when {
            lower.endsWith(".html") -> "text/html"
            lower.endsWith(".js") || lower.endsWith(".mjs") -> "application/javascript"
            lower.endsWith(".css") -> "text/css"
            lower.endsWith(".svg") -> "image/svg+xml"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".json") -> "application/json"
            lower.endsWith(".woff2") -> "font/woff2"
            lower.endsWith(".woff") -> "font/woff"
            lower.endsWith(".ttf") -> "font/ttf"
            lower.endsWith(".ico") -> "image/x-icon"
            else -> "application/octet-stream"
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
