package com.mamxanhtube.tv

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var rootContainer: FrameLayout
    private lateinit var webView: WebView
    private lateinit var errorOverlay: LinearLayout
    private lateinit var errorMessageView: TextView
    private var customFullscreenView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var lastBackPressedMs: Long = 0L

    private val localVirtualHost = "appassets.androidplatform.net"

    private val assetLoader: WebViewAssetLoader by lazy {
        WebViewAssetLoader.Builder()
            .setDomain(localVirtualHost)
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    private val customRemoteUrl: String by lazy {
        BuildConfig.APP_URL.trim()
    }

    private val allowedRemoteHost: String by lazy {
        if (customRemoteUrl.isNotEmpty()) {
            Uri.parse(customRemoteUrl).host.orEmpty().lowercase()
        } else {
            ""
        }
    }

    private fun isAndroidTvDevice(): Boolean {
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as? UiModeManager
        return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    private fun resolveStartUrl(): String {
        if (customRemoteUrl.startsWith("https://")) {
            return customRemoteUrl
        }
        return if (isAndroidTvDevice()) {
            "https://$localVirtualHost/index.html?tv=1"
        } else {
            "https://$localVirtualHost/index.html"
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (isAndroidTvDevice()) {
            enableImmersiveTvMode()
        }

        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#F6F8F5"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#F6F8F5"))
            isFocusable = true
            isFocusableInTouchMode = true

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
                // Hardened WebView boundaries for child safety
                allowFileAccess = false
                allowContentAccess = false
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = false
                val deviceTag = if (isAndroidTvDevice()) "MamXanhTubeAndroidTV/1.1" else "MamXanhTubeAndroid/1.1"
                userAgentString = "$userAgentString $deviceTag"
            }

            addJavascriptInterface(NativeYouTubeBridge(isAndroidTvDevice()), "MamXanhNative")

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val uri = request?.url ?: return null
                    if (uri.host?.lowercase() == localVirtualHost) {
                        val response = assetLoader.shouldInterceptRequest(uri)
                        if (response != null) {
                            return response
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val uri = request?.url ?: return true
                    val scheme = uri.scheme?.lowercase().orEmpty()
                    if (scheme != "https") return true

                    val host = uri.host?.lowercase().orEmpty()
                    val isBundledLocal = host == localVirtualHost
                    val isConfiguredRemote = allowedRemoteHost.isNotEmpty() && host == allowedRemoteHost
                    val isYouTubeEmbed =
                        (host == "www.youtube-nocookie.com" || host == "www.youtube.com") &&
                            uri.path.orEmpty().startsWith("/embed/")

                    // Strictly allow only bundled app origin, optional configured host, and YouTube embeds
                    return !(isBundledLocal || isConfiguredRemote || isYouTubeEmbed)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    errorOverlay.visibility = View.GONE
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        val reason = error?.description?.toString().orEmpty()
                        errorMessageView.text =
                            "Không thể mở trang ứng dụng.\nKiểm tra kết nối Wi-Fi rồi bấm Thử lại.\n($reason)"
                        errorOverlay.visibility = View.VISIBLE
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (customFullscreenView != null) {
                        callback?.onCustomViewHidden()
                        return
                    }
                    customFullscreenView = view
                    customViewCallback = callback
                    webView.visibility = View.GONE
                    rootContainer.addView(
                        view,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    )
                    enableImmersiveTvMode()
                }

                override fun onHideCustomView() {
                    val fullscreen = customFullscreenView ?: return
                    rootContainer.removeView(fullscreen)
                    customFullscreenView = null
                    customViewCallback?.onCustomViewHidden()
                    customViewCallback = null
                    webView.visibility = View.VISIBLE
                    webView.requestFocus()
                    if (isAndroidTvDevice()) {
                        enableImmersiveTvMode()
                    }
                }
            }
        }

        errorMessageView = TextView(this).apply {
            text = "Đang gặp sự cố khi tải ứng dụng."
            textSize = 16f
            setTextColor(Color.parseColor("#17231E"))
            gravity = Gravity.CENTER
            setPadding(48, 24, 48, 32)
        }

        val retryButton = Button(this).apply {
            text = "Thử tải lại"
            setBackgroundColor(Color.parseColor("#0F766E"))
            setTextColor(Color.WHITE)
            setPadding(48, 24, 48, 24)
            setOnClickListener {
                errorOverlay.visibility = View.GONE
                webView.loadUrl(resolveStartUrl())
            }
        }

        errorOverlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F6F8F5"))
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            addView(errorMessageView)
            addView(retryButton)
        }

        rootContainer.addView(webView)
        rootContainer.addView(errorOverlay)
        setContentView(rootContainer)

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(resolveStartUrl())
        }
        webView.requestFocus()
    }

    /**
     * Hardened native YouTube fetcher strictly restricted to official HTTPS YouTube hosts.
     * Allows Parent Portal inside the standalone APK to resolve channels, playlists, and videos
     * directly from the family's device without needing an external backend server.
     */
    private class NativeYouTubeBridge(private val tvDevice: Boolean) {
        private val allowedYouTubeHosts = setOf(
            "www.youtube.com",
            "youtube.com",
            "m.youtube.com",
            "youtu.be"
        )

        @JavascriptInterface
        fun isTvDevice(): Boolean = tvDevice

        @JavascriptInterface
        fun fetchYouTube(rawUrl: String): String {
            val result = JSONObject()
            try {
                var currentUrl = rawUrl.trim()
                var hops = 0
                while (hops < 3) {
                    val uri = Uri.parse(currentUrl)
                    val scheme = uri.scheme?.lowercase().orEmpty()
                    val host = uri.host?.lowercase().orEmpty()
                    if (scheme != "https" || !allowedYouTubeHosts.contains(host)) {
                        result.put("ok", false)
                        result.put("error", "Chỉ cho phép kết nối HTTPS tới YouTube.")
                        return result.toString()
                    }

                    val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 8000
                        readTimeout = 8000
                        instanceFollowRedirects = false
                        setRequestProperty(
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
                        )
                        setRequestProperty("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8")
                        setRequestProperty(
                            "Cookie",
                            "SOCS=CAISNQgDEitib3FfaWRlbnRpdHlmcm9udGVuZHVpc2VydmVyXzIwMjMwODI5LjA3X3AwGgJlbiADGgYIgJnPpwY"
                        )
                    }

                    try {
                        val code = conn.responseCode
                        if (code in 301..308) {
                            val location = conn.getHeaderField("Location").orEmpty()
                            if (location.isBlank()) {
                                result.put("ok", false)
                                result.put("status", code)
                                result.put("error", "Chuyển hướng không hợp lệ.")
                                return result.toString()
                            }
                            currentUrl = URL(URL(currentUrl), location).toString()
                            hops++
                            continue
                        }

                        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                        val out = ByteArrayOutputStream()
                        val buf = ByteArray(8192)
                        var total = 0
                        val maxBytes = 3 * 1024 * 1024
                        if (stream != null) {
                            stream.use { input ->
                                while (total < maxBytes) {
                                    val read = input.read(buf)
                                    if (read <= 0) break
                                    val allowed = minOf(read, maxBytes - total)
                                    out.write(buf, 0, allowed)
                                    total += allowed
                                }
                            }
                        }

                        val body = out.toString(Charsets.UTF_8.name())
                        result.put("ok", code in 200..299)
                        result.put("status", code)
                        result.put("body", body)
                        return result.toString()
                    } finally {
                        conn.disconnect()
                    }
                }
                result.put("ok", false)
                result.put("error", "Quá nhiều lần chuyển hướng từ YouTube.")
            } catch (e: Exception) {
                result.put("ok", false)
                result.put("error", "Không thể kết nối tới YouTube. Hãy kiểm tra Wi-Fi.")
            }
            return result.toString()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    dispatchRemoteKey("ArrowUp")
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    dispatchRemoteKey("ArrowDown")
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    dispatchRemoteKey("ArrowLeft")
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    dispatchRemoteKey("ArrowRight")
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (isAndroidTvDevice()) {
                        dispatchRemoteKey("Enter")
                        return true
                    }
                }
                KeyEvent.KEYCODE_BACK -> {
                    if (customFullscreenView != null) {
                        webView.webChromeClient?.onHideCustomView()
                        return true
                    }
                    dispatchRemoteKey("Escape")
                    val now = System.currentTimeMillis()
                    if (now - lastBackPressedMs < 1800) {
                        finish()
                    } else {
                        lastBackPressedMs = now
                        Toast.makeText(
                            this,
                            "Nhấn Quay lại thêm lần nữa để thoát ứng dụng",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun dispatchRemoteKey(key: String) {
        val safeKey = when (key) {
            "ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight", "Enter", "Escape" -> key
            else -> return
        }
        webView.evaluateJavascript(
            "window.dispatchEvent(new KeyboardEvent('keydown', { key: '$safeKey', bubbles: true }));",
            null
        )
    }

    private fun enableImmersiveTvMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
