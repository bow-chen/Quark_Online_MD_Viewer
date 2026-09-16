package com.example.quarkmdreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.quarkmdreader.data.api.QuarkApiService
import com.example.quarkmdreader.data.model.QuarkFile
import com.example.quarkmdreader.ui.screen.FileExplorerScreen
import com.example.quarkmdreader.ui.screen.MarkdownReaderScreen
import com.example.quarkmdreader.ui.screen.QuarkLoginScreen

sealed class Screen {
    object Login : Screen()
    object Explorer : Screen()
    data class Reader(val file: QuarkFile, val currentDirFid: String) : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var quarkApi: QuarkApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        quarkApi = QuarkApiService(applicationContext)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember {
                        mutableStateOf<Screen>(
                            if (quarkApi.isLoggedIn()) Screen.Explorer else Screen.Login
                        )
                    }

                    when (val screen = currentScreen) {
                        is Screen.Login -> {
                            QuarkLoginScreen(
                                onLoginSuccess = { cookie ->
                                    quarkApi.saveCookie(cookie)
                                    currentScreen = Screen.Explorer
                                }
                            )
                        }
                        is Screen.Explorer -> {
                            FileExplorerScreen(
                                quarkApi = quarkApi,
                                onOpenMarkdown = { file, dirFid ->
                                    currentScreen = Screen.Reader(file, dirFid)
                                },
                                onLogout = {
                                    quarkApi.clearLogin()
                                    currentScreen = Screen.Login
                                }
                            )
                        }
                        is Screen.Reader -> {
                            MarkdownReaderScreen(
                                file = screen.file,
                                currentDirFid = screen.currentDirFid,
                                quarkApi = quarkApi,
                                onBack = {
                                    currentScreen = Screen.Explorer
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
