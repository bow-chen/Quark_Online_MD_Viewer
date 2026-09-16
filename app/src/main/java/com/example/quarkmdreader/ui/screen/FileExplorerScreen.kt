package com.example.quarkmdreader.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.quarkmdreader.data.api.QuarkApiService
import com.example.quarkmdreader.data.model.QuarkFile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    quarkApi: QuarkApiService,
    onOpenMarkdown: (file: QuarkFile, currentDirFid: String) -> Unit,
    onLogout: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // 目录导航栈 (fid, dirName)
    val dirStack = remember { mutableStateListOf(Pair("0", "根目录")) }
    val currentDir = dirStack.last()

    var files by remember { mutableStateOf<List<QuarkFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadCurrentFiles() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val result = quarkApi.getFileList(currentDir.first)
            isLoading = false
            if (result.isSuccess) {
                files = result.getOrNull() ?: emptyList()
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "加载文件失败"
            }
        }
    }

    LaunchedEffect(currentDir.first) {
        loadCurrentFiles()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentDir.second) },
                navigationIcon = {
                    if (dirStack.size > 1) {
                        IconButton(onClick = { dirStack.removeAt(dirStack.size - 1) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回上一级")
                        }
                    } else {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.padding(start = 12.dp))
                    }
                },
                actions = {
                    IconButton(onClick = { loadCurrentFiles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "退出登录")
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
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { loadCurrentFiles() }) {
                            Text("重试")
                        }
                    }
                }
                files.isEmpty() -> {
                    Text(
                        text = "此目录下没有文件",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(files) { file ->
                            FileListItem(
                                file = file,
                                onClick = {
                                    if (file.isDir) {
                                        dirStack.add(Pair(file.fid, file.fileName))
                                    } else if (file.isMarkdown) {
                                        onOpenMarkdown(file, currentDir.first)
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileListItem(
    file: QuarkFile,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when {
            file.isDir -> Icons.Default.Folder
            file.isMarkdown -> Icons.Default.Article
            file.isImage -> Icons.Default.Image
            else -> Icons.Default.InsertDriveFile
        }

        val iconTint = when {
            file.isDir -> MaterialTheme.colorScheme.primary
            file.isMarkdown -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.outline
        }

        Icon(icon, contentDescription = null, tint = iconTint)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = file.fileName, style = MaterialTheme.typography.bodyLarge)
            if (!file.isDir) {
                Text(
                    text = "${file.size / 1024} KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        if (file.isDir) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}
