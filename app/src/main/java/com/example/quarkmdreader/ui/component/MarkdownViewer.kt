package com.example.quarkmdreader.ui.component

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.quarkmdreader.data.api.QuarkApiService
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarkdownViewer(
    markdownText: String,
    currentDirFid: String,
    quarkApi: QuarkApiService,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isPageLoaded by remember { mutableStateOf(false) }

    // 当 markdownText 或主题发生变化时，调用 JS 函数更新页面内容
    LaunchedEffect(markdownText, isDark, isPageLoaded) {
        if (isPageLoaded && webViewInstance != null) {
            val escapedText = JSONObject.quote(markdownText)
            webViewInstance?.evaluateJavascript("renderMarkdown($escapedText, $isDark);", null)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isPageLoaded = true
                        val escapedText = org.json.JSONObject.quote(markdownText)
                        view?.evaluateJavascript("renderMarkdown($escapedText, $isDark);", null)
                    }

                    // 拦截图片等相对路径资源，自动从夸克网盘流式加载
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val uri = request?.url ?: return null
                        val scheme = uri.scheme ?: ""
                        val path = uri.path ?: ""

                        // 拦截相对路径资源（例如 images/... 或 custom-quark://...）
                        if (scheme == "quark-img" || (scheme == "file" && !path.contains("android_asset"))) {
                            val relativePath = uri.toString()
                                .replace("quark-img://", "")
                                .replace("file:///", "")
                                .replace("file://", "")

                            return fetchQuarkImageResponse(quarkApi, currentDirFid, relativePath)
                        }

                        // 如果是 marked 默认解析生成的相对路径请求（如 https://local.quark/xxx 或 http://...）
                        if (uri.host == "local.quark" || uri.toString().startsWith("http://localhost/")) {
                            val cleanPath = uri.path?.removePrefix("/") ?: ""
                            return fetchQuarkImageResponse(quarkApi, currentDirFid, cleanPath)
                        }

                        return super.shouldInterceptRequest(view, request)
                    }
                }

                loadUrl("file:///android_asset/math_markdown_template.html")
                webViewInstance = this
            }
        },
        update = { webView ->
            if (isPageLoaded) {
                val escapedText = org.json.JSONObject.quote(markdownText)
                webView.evaluateJavascript("renderMarkdown($escapedText, $isDark);", null)
            }
        }
    )
}

/**
 * 流式获取夸克网盘相对路径图片并封装为 WebResourceResponse
 */
private fun fetchQuarkImageResponse(
    quarkApi: QuarkApiService,
    currentDirFid: String,
    relativePath: String
): WebResourceResponse? {
    return try {
        runBlocking(Dispatchers.IO) {
            val fid = quarkApi.findFileFidByRelativePath(currentDirFid, relativePath) ?: return@runBlocking null
            val inputStream = quarkApi.getFileInputStream(fid) ?: return@runBlocking null
            val mimeType = when {
                relativePath.endsWith(".png", true) -> "image/png"
                relativePath.endsWith(".jpg", true) || relativePath.endsWith(".jpeg", true) -> "image/jpeg"
                relativePath.endsWith(".gif", true) -> "image/gif"
                relativePath.endsWith(".svg", true) -> "image/svg+xml"
                relativePath.endsWith(".webp", true) -> "image/webp"
                else -> "image/*"
            }
            WebResourceResponse(mimeType, "UTF-8", inputStream)
        }
    } catch (e: Exception) {
        null
    }
}