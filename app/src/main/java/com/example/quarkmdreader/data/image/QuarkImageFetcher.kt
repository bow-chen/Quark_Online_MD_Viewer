package com.example.quarkmdreader.data.image

import android.content.Context
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.example.quarkmdreader.data.api.QuarkApiService
import okio.buffer
import okio.source

/**
 * 包装夸克相对路径图片请求对象
 */
data class QuarkImageRequest(
    val currentDirFid: String,
    val relativePath: String
)

class QuarkImageFetcher(
    private val data: QuarkImageRequest,
    private val quarkApi: QuarkApiService,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        // 1. 在夸克网盘异步检索相对路径对应的文件 fid
        val fid = quarkApi.findFileFidByRelativePath(data.currentDirFid, data.relativePath)
            ?: return null

        // 2. 获取该图片的二进制输入流
        val inputStream = quarkApi.getFileInputStream(fid) ?: return null

        // 3. 构建 Coil 的内存 Source，直接解码绘制，不存入本地存储
        val source = inputStream.source().buffer()
        return SourceResult(
            source = ImageSource(source, options.context),
            mimeType = null,
            dataSource = DataSource.NETWORK
        )
    }

    class Factory(private val quarkApi: QuarkApiService) : Fetcher.Factory<QuarkImageRequest> {
        override fun create(data: QuarkImageRequest, options: Options, imageLoader: ImageLoader): Fetcher {
            return QuarkImageFetcher(data, quarkApi, options)
        }
    }
}
