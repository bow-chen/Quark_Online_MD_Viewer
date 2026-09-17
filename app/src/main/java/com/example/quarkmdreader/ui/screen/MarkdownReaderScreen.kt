package com.example.quarkmdreader.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.quarkmdreader.data.api.QuarkApiService
import com.example.quarkmdreader.data.model.QuarkFile
import com.example.quarkmdreader.ui.component.MarkdownViewer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownReaderScreen(
    file: QuarkFile,
    currentDirFid: String,
    quarkApi: QuarkApiService,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var markdownContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadContent() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val result = quarkApi.readMarkdownContent(file.fid)
            isLoading = false
            if (result.isSuccess) {
                markdownContent = result.getOrNull()
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "无法加载 Markdown 内容"
            }
        }
    }

    LaunchedEffect(file.fid) {
        loadContent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = file.fileName,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { loadContent() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("正在从夸克网盘流式加载中...")
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { loadContent() }) {
                            Text("重试")
                        }
                    }
                }
                markdownContent != null -> {
                    MarkdownViewer(
                        markdownText = markdownContent ?: "",
                        currentDirFid = currentDirFid,
                        quarkApi = quarkApi,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
