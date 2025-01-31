package com.metrolist.music.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.AccountChannelHandleKey
import com.metrolist.music.constants.AccountEmailKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.VisitorDataKey
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.reportException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

private const val YOUTUBE_MUSIC_URL = "https://music.youtube.com"

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun LoginScreen(navController: NavController) {
    var visitorData by rememberPreference(VisitorDataKey, "")
    var innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    var accountName by rememberPreference(AccountNameKey, "")
    var accountEmail by rememberPreference(AccountEmailKey, "")
    var accountChannelHandle by rememberPreference(AccountChannelHandleKey, "")

    var webView: WebView? = null

    AndroidView(
        modifier =
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                        Timber.tag("WebView").d("Page started: $url")
                        super.onPageStarted(view, url, favicon)
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        Timber.tag("WebView").d("Page finished: $url")
                        if (url != null && url.startsWith(YOUTUBE_MUSIC_URL)) {
                            val youTubeCookieString = CookieManager.getInstance().getCookie(url)
                            innerTubeCookie = if ("SAPISID" in parseCookieString(youTubeCookieString)) youTubeCookieString else ""
                            if (innerTubeCookie.isNotEmpty()) {
                                GlobalScope.launch {
                                    YouTube.accountInfo().onSuccess {
                                        accountName = it.name
                                        accountEmail = it.email.orEmpty()
                                        accountChannelHandle = it.channelHandle.orEmpty()
                                        Timber.tag("WebView").d("Account info retrieved: $accountName, $accountEmail, $accountChannelHandle")
                                    }.onFailure {
                                        reportException(it)
                                        Timber.tag("WebView").e(it, "Failed to retrieve account info")
                                    }
                                }
                            } else {
                                Timber.tag("WebView").e("Failed to retrieve InnerTube cookie")
                            }
                            loadUrl("javascript:Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA)")
                        }
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val url = request.url.toString()
                        Timber.tag("WebView").d("Loading URL: $url")
                        return super.shouldOverrideUrlLoading(view, request)
                    }
                }
                settings.apply {
                    javaScriptEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                }
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onRetrieveVisitorData(newVisitorData: String?) {
                            if (innerTubeCookie == "") {
                                visitorData = ""
                                Timber.tag("WebView").e("InnerTube cookie is empty, cannot retrieve visitor data")
                                return
                            }

                            if (newVisitorData != null) {
                                visitorData = newVisitorData
                                Timber.tag("WebView").d("Visitor data retrieved: $visitorData")
                            }
                        }
                    },
                    "Android",
                )
                webView = this
                loadUrl(
                    "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=$YOUTUBE_MUSIC_URL",
                )
            }
        },
    )

    TopAppBar(
        title = { Text(stringResource(R.string.login)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}
