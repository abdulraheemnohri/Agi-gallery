package com.google.ai.edge.gallery.customtasks.tinygarden

import android.content.Intent
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "AGTGContent"
private const val ASSETS_BASE_URL = "http://appassets.androidplatform.net"

@Composable
fun TinyGardenContent(
    viewModel: TinyGardenViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var webViewRef: WebView? by remember { mutableStateOf(null) }

    Box(contentAlignment = Alignment.BottomCenter, modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxHeight(),
            factory = { context ->
                val assetLoader =
                    WebViewAssetLoader.Builder()
                        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                        .build()

                WebView(context).apply {
                    webViewRef = this
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        mediaPlaybackRequiresUserGesture = false
                    }

                    webViewClient =
                        object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest,
                            ): WebResourceResponse? {
                                return assetLoader.shouldInterceptRequest(request.url)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (!viewModel.dataStoreRepository.getHasRunTinyGarden()) {
                                    viewModel.dataStoreRepository.setHasRunTinyGarden(true)
                                    scope.launch {
                                        delay(1000)
                                        webViewRef
                                            ?.runCatching { evaluateJavascript("tinyGarden.showHelp()", null) }
                                            ?.onFailure { e -> Log.e(TAG, "$e") }
                                    }
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean {
                                if (request == null) return false
                                val url = request.url.toString()
                                if (url.startsWith(ASSETS_BASE_URL)) return false
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                    view?.context?.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Could not open external URL: $url", e)
                                }
                                return true
                            }
                        }

                    webChromeClient =
                        object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                Log.d(
                                    TAG,
                                    "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}",
                                )
                                return super.onConsoleMessage(consoleMessage)
                            }
                        }

                    var url = "$ASSETS_BASE_URL/assets/tinygarden/index.html"
                    if (!viewModel.dataStoreRepository.getHasRunTinyGarden()) {
                        viewModel.dataStoreRepository.setHasRunTinyGarden(true)
                        url = "$url?tutorial=1"
                    }
                    loadUrl(url)
                }
            },
        )
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(bottom = 12.dp))
    }
}
