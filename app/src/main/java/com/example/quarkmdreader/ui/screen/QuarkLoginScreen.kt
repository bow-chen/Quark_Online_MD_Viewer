package com.example.quarkmdreader.ui.screen

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun QuarkLoginScreen(
    onLoginSuccess: (cookie: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 手动输入 Cookie, 1: 网页自动登录
    var manualCookieText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    var isWebLoading by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("配置夸克网盘账号") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("手动粘贴 Cookie (推荐)") },
                    icon = { Icon(Icons.Default.ContentPaste, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("网页扫码登录") },
                    icon = { Icon(Icons.Default.Language, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                // 手动输入 Cookie 模式
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "请粘贴你的夸克网盘 Cookie：",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = manualCookieText,
                        onValueChange = { manualCookieText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("在此粘贴形如 _UP_A4A_11_=...; __pus=...; __puus=... 的完整 Cookie") },
                        maxLines = 10
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    manualCookieText = clip.trim()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("从剪贴板粘贴")
                        }

                        Button(
                            onClick = {
                                val clean = manualCookieText.trim()
                                if (clean.isNotEmpty()) {
                                    onLoginSuccess(clean)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = manualCookieText.isNotBlank()
                        ) {
                            Text("确认保存并进入")
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "💡 Cookie 获取小技巧：",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. 浏览器打开 pan.quark.cn 登录账号\n2. F12 打开开发者工具 -> Network (网络)\n3. 找到任意接口请求，复制 Request Headers 中的 Cookie\n4. 或者切换到上方【网页扫码登录】页面直接登录",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                // 网页登录模式
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 Chrome/114.0.0.0 Mobile Safari/537.36"

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isWebLoading = false

                                        url?.let {
                                            val cookie = CookieManager.getInstance().getCookie(it) ?: ""
                                            if (cookie.contains("__pus=") && (cookie.contains("__puus=") || cookie.contains("token="))) {
                                                onLoginSuccess(cookie)
                                            }
                                        }
                                    }
                                }
                                loadUrl("https://pan.quark.cn/")
                            }
                        }
                    )

                    if (isWebLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}
