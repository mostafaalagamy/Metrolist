package com.metrolist.music.ui.screens.settings

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.DiscordTokenKey
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordLoginScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var discordToken by rememberPreference(DiscordTokenKey, "")
    var webView: WebView? = null

    AndroidView(
        modifier = Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                WebView.setWebContentsDebuggingEnabled(true)

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true

                CookieManager.getInstance().apply {
                    removeAllCookies(null)
                    flush()
                }

                WebStorage.getInstance().deleteAllData()

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onRetrieveToken(token: String) {
                        Timber.d("Token: %s", token)
                        if (token != "null" && token != "error") {
                            discordToken = token
                            scope.launch(Dispatchers.Main) {
                                webView?.loadUrl("about:blank")
                                navController.navigateUp()
                            }
                        }
                    }
                }, "Android")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        if (url.contains("/channels/@me") || url.contains("/app")) {
                            view.evaluateJavascript(
                                """
                                (function() {
                                    try {
                                        var token = localStorage.getItem("token");
                                        if (token) {
                                            Android.onRetrieveToken(token.slice(1, -1));
                                        } else {
                                            // fallback إلى alert (منطق kizzy)
                                            var i = document.createElement('iframe');
                                            document.body.appendChild(i);
                                            setTimeout(function() {
                                                try {
                                                    var alt = i.contentWindow.localStorage.token;
                                                    if (alt) {
                                                        alert(alt.slice(1, -1));
                                                    } else {
                                                        alert("null");
                                                    }
                                                } catch (e) {
                                                    alert("error");
                                                }
                                            }, 1000);
                                        }
                                    } catch (e) {
                                        alert("error");
                                    }
                                })();
                                """.trimIndent(), null
                            )
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean = false
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onJsAlert(
                        view: WebView,
                        url: String,
                        message: String,
                        result: JsResult
                    ): Boolean {
                        if (message != "null" && message != "error") {
                            discordToken = message
                            scope.launch(Dispatchers.Main) {
                                view.loadUrl("about:blank")
                                navController.navigateUp()
                            }
                        }
                        result.confirm()
                        return true
                    }
                }

                webView = this
                loadUrl("https://discord.com/login")
            }
        }
    )

    TopAppBar(
        title = { Text(stringResource(R.string.action_login)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        }
    )

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}
