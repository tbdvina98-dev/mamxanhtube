package com.mamxanhtube.tv

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var rootContainer: FrameLayout
    private lateinit var webView: WebView
    private var customFullscreenView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var lastBackPressedMs: Long = 0L

    private val allowedHost: String by lazy {
        Uri.parse(BuildConfig.APP_URL).host.orEmpty().lowercase()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableImmersiveTvMode()

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
                userAgentString = "$userAgentString MamXanhTubeAndroidTV/1.0"
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val uri = request?.url ?: return true
                    val scheme = uri.scheme?.lowercase().orEmpty()
                    if (scheme != "https") return true

                    val host = uri.host?.lowercase().orEmpty()
                    val isAppOrigin = host == allowedHost
                    val isYouTubeEmbed =
                        (host == "www.youtube-nocookie.com" || host == "www.youtube.com") &&
                            uri.path.orEmpty().startsWith("/embed/")

                    // Allow only the Mầm Xanh Tube origin and privacy-enhanced YouTube embeds
                    return !(isAppOrigin || isYouTubeEmbed)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        Toast.makeText(
                            this@MainActivity,
                            "Không có kết nối mạng. Vui lòng kiểm tra Wi-Fi của TV.",
                            Toast.LENGTH_LONG
                        ).show()
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
                    enableImmersiveTvMode()
                }
            }
        }

        rootContainer.addView(webView)
        setContentView(rootContainer)

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(BuildConfig.APP_URL)
        }
        webView.requestFocus()
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
                    dispatchRemoteKey("Enter")
                    return true
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
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
