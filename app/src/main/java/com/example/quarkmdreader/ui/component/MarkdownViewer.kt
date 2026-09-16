package com.example.quarkmdreader.ui.component

import android.content.Context
import android.widget.TextView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import com.example.quarkmdreader.data.api.QuarkApiService
import com.example.quarkmdreader.data.image.QuarkImageFetcher
import com.example.quarkmdreader.data.image.QuarkImageRequest
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.coil.CoilImagesPlugin

@Composable
fun MarkdownViewer(
    markdownText: String,
    currentDirFid: String,
    quarkApi: QuarkApiService,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val textView = TextView(ctx).apply {
                setTextIsSelectable(true)
                textSize = 15f
                setLineSpacing(6f, 1.2f)
            }

            val imageLoader = ImageLoader.Builder(ctx)
                .components {
                    add(QuarkImageFetcher.Factory(quarkApi))
                }
                .crossfade(true)
                .build()

            val markwon = Markwon.builder(ctx)
                .usePlugin(TablePlugin.create(ctx))
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TaskListPlugin.create(ctx))
                .usePlugin(
                    CoilImagesPlugin.create(
                        object : CoilImagesPlugin.CoilStore {
                            override fun load(drawable: AsyncDrawable): coil.request.ImageRequest {
                                val destination = drawable.destination
                                val requestData = if (destination.startsWith("http://") || destination.startsWith("https://")) {
                                    destination
                                } else {
                                    QuarkImageRequest(currentDirFid, destination)
                                }
                                return coil.request.ImageRequest.Builder(ctx)
                                    .data(requestData)
                                    .build()
                            }

                            override fun cancel(drawable: AsyncDrawable) {
                                // 退出视图或滑动走时取消加载，节约内存
                            }
                        },
                        imageLoader
                    )
                )
                .build()

            markwon.setMarkdown(textView, markdownText)
            textView
        },
        update = { textView ->
            textView.setTextColor(if (isDark) 0xFFEEEEEE.toInt() else 0xFF222222.toInt())
        }
    )
}
